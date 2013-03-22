
package com.akiban.sql.optimizer.rule;

import com.akiban.server.types3.Types3Switch;
import com.akiban.sql.NamedParamsTestBase;
import com.akiban.sql.TestBase;

import com.akiban.sql.optimizer.NestedResultSetTypeComputer;
import com.akiban.sql.optimizer.OptimizerTestBase;
import com.akiban.sql.optimizer.plan.AST;
import com.akiban.sql.optimizer.plan.PlanToString;
import com.akiban.sql.optimizer.rule.PlanContext;

import com.akiban.sql.parser.DMLStatementNode;
import com.akiban.sql.parser.StatementNode;

import com.akiban.ais.model.AkibanInformationSchema;
import com.akiban.server.service.functions.FunctionsRegistryImpl;

import com.akiban.junit.NamedParameterizedRunner;
import com.akiban.junit.NamedParameterizedRunner.TestParameters;
import com.akiban.junit.Parameterization;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;

@RunWith(NamedParameterizedRunner.class)
public class RulesTest extends OptimizerTestBase
                       implements TestBase.GenerateAndCheckResult
{
    public static final File RESOURCE_DIR = 
        new File(OptimizerTestBase.RESOURCE_DIR, "rule");

    protected File rulesFile, schemaFile, indexFile, statsFile, propertiesFile, extraDDL;

    @TestParameters
    public static Collection<Parameterization> statements() throws Exception {
        Collection<Object[]> result = new ArrayList<>();
        for (File subdir : RESOURCE_DIR.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            })) {
            File rulesFile;
            if (Types3Switch.ON) {
                rulesFile = new File (subdir, "t3rules.yml");
                if (!rulesFile.exists()) 
                    rulesFile = new File (subdir, "rules.yml");
            } else {
                rulesFile = new File(subdir, "rules.yml");
            }
            File schemaFile = new File(subdir, "schema.ddl");
            if (rulesFile.exists() && schemaFile.exists()) {
                File defaultStatsFile = new File(subdir, "stats.yaml");
                File defaultPropertiesFile = new File(subdir, "compiler.properties");
                File defaultExtraDDL = new File(subdir, "schema-extra.ddl");
                if (!defaultStatsFile.exists())
                    defaultStatsFile = null;
                if (!defaultPropertiesFile.exists())
                    defaultPropertiesFile = null;
                if (!defaultExtraDDL.exists())
                    defaultExtraDDL = null;
                for (Object[] args : sqlAndExpected(subdir)) {
                    File statsFile = new File(subdir, args[0] + ".stats.yaml");
                    File propertiesFile = new File(subdir, args[0] + ".properties");
                    File extraDDL = new File(subdir, args[0] + ".ddl");
                    if (!statsFile.exists())
                        statsFile = defaultStatsFile;
                    if (!propertiesFile.exists())
                        propertiesFile = defaultPropertiesFile;
                    if (!extraDDL.exists())
                        extraDDL = defaultExtraDDL;
                    File t3Results = new File (subdir, args[0] + ".t3expected");
                    if (t3Results.exists() && Types3Switch.ON) {
                        args[2] = fileContents(t3Results);
                    }
                    Object[] nargs = new Object[args.length+5];
                    nargs[0] = subdir.getName() + "/" + args[0];
                    nargs[1] = rulesFile;
                    nargs[2] = schemaFile;
                    nargs[3] = statsFile;
                    nargs[4] = propertiesFile;
                    nargs[5] = extraDDL;
                    System.arraycopy(args, 1, nargs, 6, args.length-1);
                    result.add(nargs);
                }
            }
        }
        return NamedParamsTestBase.namedCases(result);
    }

    public RulesTest(String caseName, 
                     File rulesFile, File schemaFile, File statsFile, File propertiesFile,
                     File extraDDL,
                     String sql, String expected, String error) {
        super(caseName, sql, expected, error);
        this.rulesFile = rulesFile;
        this.schemaFile = schemaFile;
        this.statsFile = statsFile;
        this.propertiesFile = propertiesFile;
        this.extraDDL = extraDDL;
    }

    protected RulesContext rules;

    @Before
    public void loadDDL() throws Exception {
        List<File> schemaFiles = new ArrayList<>(2);
        schemaFiles.add(schemaFile);
        if (extraDDL != null)
            schemaFiles.add(extraDDL);
        AkibanInformationSchema ais = loadSchema(schemaFiles);
        Properties properties = new Properties();
        if (propertiesFile != null) {
            FileInputStream fstr = new FileInputStream(propertiesFile);
            try {
                properties.load(fstr);
            }
            finally {
                fstr.close();
            }
        }
        rules = RulesTestContext.create(ais, statsFile, extraDDL != null,
                                        RulesTestHelper.loadRules(rulesFile), 
                                        properties);
        // Normally set as a consequence of OutputFormat.
        if (Boolean.parseBoolean(properties.getProperty("allowSubqueryMultipleColumns",
                                                        "false"))) {
            binder.setAllowSubqueryMultipleColumns(true);
            typeComputer = new NestedResultSetTypeComputer(new FunctionsRegistryImpl());
        }
    }

    @Test
    public void testRules() throws Exception {
        generateAndCheckResult();
    }

    @Override
    public String generateResult() throws Exception {
        StatementNode stmt = parser.parseStatement(sql);
        binder.bind(stmt);
        stmt = booleanNormalizer.normalize(stmt);
        typeComputer.compute(stmt);
        stmt = subqueryFlattener.flatten((DMLStatementNode)stmt);
        // Turn parsed AST into intermediate form as starting point.
        PlanContext plan = new PlanContext(rules, 
                                           new AST((DMLStatementNode)stmt,
                                                   parser.getParameterList()));
        rules.applyRules(plan);
        return PlanToString.of(plan.getPlan());
    }

    @Override
    public void checkResult(String result) throws IOException {
        assertEqualsWithoutHashes(caseName, expected, result);
    }

}
