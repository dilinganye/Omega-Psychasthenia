package data.scripts.console.commands;
import java.util.*;
import java.lang.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class GoToCurr implements BaseCommand{
    @Override
    public BaseCommand.CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            Console.showMessage("You need a location Vector2D.");
            return CommandResult.ERROR;
        }
        final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        StringTokenizer str2 = new StringTokenizer(args, ",");
        int i = 0;
        Float x,y;
        x = (playerFleet.getLocation().x);
        y = (playerFleet.getLocation().y);
        while(str2.hasMoreTokens()){
            i += 1;
            if(i<=1){
                x = Float.valueOf(str2.nextToken().replaceAll("\\D", ""));
            } else if (i<=2) {
                y = Float.valueOf(str2.nextToken().replaceAll("\\D", ""));
            }
            //System.out.println(str2.nextToken());
        }

        playerFleet.setLocation(x, y);
        playerFleet.clearAssignments();
        return CommandResult.SUCCESS;
    }
}
