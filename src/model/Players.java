package model;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Players {
    private ArrayList<Player> players;

    public Players() {
        this.players = new ArrayList<>();
    }

    public Players(ArrayList<Player> players) {
        this.players = players;
    }

    @XmlElement(name = "player")
    public ArrayList<Player> getPlayers() {
        return players;
    }

    public void setPlayers(ArrayList<Player> players) {
        this.players = players;
    }
}
