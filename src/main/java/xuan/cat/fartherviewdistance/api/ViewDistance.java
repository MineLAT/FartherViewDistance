package xuan.cat.fartherviewdistance.api;

import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.api.data.PlayerView;
import xuan.cat.fartherviewdistance.FVD;
import xuan.cat.fartherviewdistance.core.data.PlayerChunkView;

public final class ViewDistance {
    private ViewDistance() {
    }


    public static PlayerView getPlayerView(Player player) {
        PlayerChunkView view = FVD.getChunkServer().getView(player);
        return view != null ? view.viewAPI : null;
    }
}
