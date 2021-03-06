package team;

import arc.*;
import arc.struct.ObjectMap;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.world.Tile;

// use java.util for now
import java.util.Arrays;

public class TeamPlugin extends Plugin {
    //private boolean DEBUG = false;
    private long TEAM_CD = 5000L;

    private ObjectMap<Player, Long> teamTimers = new ObjectMap<>();

    private Team spectateTeam = Team.all[8];
    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();

    //register event handlers and create variables in the constructor
    public TeamPlugin(){
        Events.on(PlayerLeave.class, event -> {
            if(rememberSpectate.containsKey(event.player)){
                rememberSpectate.remove(event.player);
            }
            if(teamTimers.containsKey(event.player)){
                teamTimers.remove(event.player);
            }
        });

    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("team", "change team - cooldown", (args, player) ->{
            if(rememberSpectate.containsKey(player)){
                player.sendMessage(">[orange] transferring back to last team");
                player.team(rememberSpectate.get(player));
                Call.setPlayerTeamEditor(player, rememberSpectate.get(player));
                rememberSpectate.remove(player);
                return;
            }
            if(Vars.state.rules.tags.getBool("forceTeam") && !player.admin()){
                player.sendMessage("You can't change teams. An admin activated forceTeam!");
                return;
            }

            if(System.currentTimeMillis() < teamTimers.get(player,0L)){
                player.sendMessage(">[orange] command is on a 5 second cooldown...");
                return;
            }
            coreTeamReturn ret = getPosTeamLoc(player);
            if(ret != null) {
                Call.setPlayerTeamEditor(player, ret.team);
                player.team(ret.team);
                //maybe not needed
                Call.setPosition(player.con, ret.x, ret.y);
                player.unit().set(ret.x, ret.y);
                player.snapSync();
                teamTimers.put(player, System.currentTimeMillis()+TEAM_CD);
                Call.sendMessage(String.format("> %s []changed to team [sky]%s", player.name, ret.team));
            }else{
                player.sendMessage("[scarlet]You can't change teams ...");
            }
        });

        handler.<Player>register("spectate", "[scarlet]Admin only[]", (args, player) -> {
            if(!player.admin()){
               player.sendMessage("[scarlet]This command is only for admins.");
               return;
            }
            if(rememberSpectate.containsKey(player)){
                player.team(rememberSpectate.get(player));
                Call.setPlayerTeamEditor(player, rememberSpectate.get(player));
                rememberSpectate.remove(player);
                player.sendMessage("[gold]PLAYER MODE[]");
            }else{
                rememberSpectate.put(player, player.unit().team);
                player.team(spectateTeam);
                Call.setPlayerTeamEditor(player, spectateTeam);
                player.unit().kill();
                player.sendMessage("[green]SPECTATE MODE[]");
                player.sendMessage("use /team or /spectate to go back to player mode");
            }
        });
    }
    //search a possible team
    private Team getPosTeam(Player p){
        Team currentTeam = p.team();
        int c_index = Arrays.asList(Team.baseTeams).indexOf(currentTeam);
        int i = (c_index+1)%6;
        while (i != c_index){
            if (Team.baseTeams[i].cores().size > 0){
                return Team.baseTeams[i];
            }
            i = (i + 1) % Team.baseTeams.length;
        }
        return currentTeam;
    }

    private coreTeamReturn getPosTeamLoc(Player p){
        Team currentTeam = p.team();
        Team newTeam = getPosTeam(p);
        if (newTeam == currentTeam){
            return null;
        }else{
            Tile coreTile = newTeam.core().tileOn();
            return new coreTeamReturn(newTeam, coreTile.drawx(), coreTile.drawy());
        }
    }

    class coreTeamReturn{
        Team team;
        float x,y;
        public coreTeamReturn(Team _t, float _x, float _y){
            team = _t;
            x = _x;
            y = _y;
        }
    }
}
