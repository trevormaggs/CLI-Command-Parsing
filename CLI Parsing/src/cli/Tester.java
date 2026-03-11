package cli;

import java.text.ParseException;

public class Tester
{
    public static void testOne()
    {
        String[] dummy = {"--platform", "=", "rhel,win10,win2016,WIN2012R2,WIN2019,WIN2022,sles,ubn", "-o", "scopeos.txt", "MDAV Details Export.csv", "--temp", "", "-5.7,-0.5,-4.2, 8.2"};

        CommandFlagParser cli = new CommandFlagParser(dummy);

        cli.setDebug(true);
        cli.setFreeArgumentLimit(2);

        try
        {
            cli.addDefinition("-u", FlagType.ARG_OPTIONAL);
            cli.addDefinition("-f", FlagType.ARG_OPTIONAL);
            cli.addDefinition("-o", FlagType.ARG_OPTIONAL);
            cli.addDefinition("-p", FlagType.SEP_OPTIONAL);
            cli.addDefinition("--platform", FlagType.SEP_OPTIONAL);
            cli.addDefinition("-s", FlagType.ARG_BLANK);
            cli.addDefinition("--temp", FlagType.ARG_REQUIRED);

            cli.addDefinition("-d", FlagType.ARG_BLANK);
            cli.addDefinition("--debug", FlagType.ARG_BLANK);

            cli.addDefinition("-h", FlagType.ARG_BLANK);
            cli.addDefinition("--help", FlagType.ARG_BLANK);

            cli.parse();
        }

        catch (ParseException exc)
        {
            System.err.println("Error: " + exc.getMessage());
            // System.err.println(cli.generateUsage());
            System.exit(1);
        }
    }

    public static void testTwo()
    {
        String[] dummy = {
                "-g", "vulfeed.dat", "-acp31", "33", "--gem787", "--csv", "val99", "--depth82",
                "-b=727", "-n", "/var/trigger.xlsx", "-b", "=", "747", "-vofile.xlsx", "-h", "-x",
                "nina", "-k707", "--range=12,24,36,48,60,72", ",84", ",", ",,,", "96,", ",", ",",
                "--query", "=", "80286", "--range=", ",,,108", "outcomes", "D:/KDR Project/Milestones/TestBatch"};

        CommandFlagParser cli = new CommandFlagParser(dummy);

        cli.setDebug(true);
        cli.setFreeArgumentLimit(3);

        try
        {
            cli.addDefinition("-a", FlagType.ARG_BLANK);
            cli.addDefinition("-c", FlagType.ARG_REQUIRED);
            cli.addDefinition("--gem", FlagType.ARG_REQUIRED);
            cli.addDefinition("-x", FlagType.ARG_REQUIRED);
            cli.addDefinition("-g", FlagType.ARG_REQUIRED);
            cli.addDefinition("-n", FlagType.ARG_REQUIRED);
            cli.addDefinition("-o", FlagType.ARG_REQUIRED);
            cli.addDefinition("-v", FlagType.ARG_BLANK);
            cli.addDefinition("-h", FlagType.ARG_BLANK);
            cli.addDefinition("--help", FlagType.ARG_BLANK);
            cli.addDefinition("--csv", FlagType.ARG_OPTIONAL);
            cli.addDefinition("-k", FlagType.ARG_OPTIONAL);
            cli.addDefinition("--depth", FlagType.ARG_REQUIRED);
            cli.addDefinition("-b", FlagType.SEP_OPTIONAL);
            cli.addDefinition("--query", FlagType.SEP_REQUIRED);
            cli.addDefinition("--range", FlagType.SEP_REQUIRED);

            cli.parse();
        }

        catch (ParseException exc)
        {
            System.out.println("Error: " + exc.getMessage());
            // System.err.println(cli.generateUsage());
            System.exit(1);
        }
    }

    public static void main(String[] args)
    {
        //testOne();
        testTwo();
    }
}