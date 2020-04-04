package remake.core.player;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Locale;

import static remake.Main.config;
import static remake.Main.tool;

public class PlayerData {
    public String name;
    public String uuid;
    public String country;
    public String country_code;
    public String language;
    public boolean isAdmin;
    public int placecount;
    public int breakcount;
    public int killcount;
    public int deathcount;
    public int joincount;
    public int kickcount;
    public int level;
    public int exp;
    public int reqexp;
    public String reqtotalexp;
    public String firstdate;
    public String lastdate;
    public String lastplacename;
    public String lastbreakname;
    public String lastchat;
    public String playtime;
    public int attackclear;
    public int pvpwincount;
    public int pvplosecount;
    public int pvpbreakout;
    public int reactorcount;
    public String bantimeset;
    public String bantime;
    public boolean banned;
    public boolean translate;
    public boolean crosschat;
    public boolean colornick;
    public boolean connected;
    public String connserver;
    public String permission;
    public boolean mute;
    public boolean alert;
    public Long udid;
    public String email;
    public String accountid;
    public String accountpw;

    public boolean error;
    public boolean isLogin;

    public LocalTime afk = LocalTime.of(0, 0, 0);
    public int afk_tilex = 0;
    public int afk_tiley = 0;

    public int grief_build_count = 0;
    public int grief_destory_count = 0;
    public ArrayList<short[]> grief_tilelist = new ArrayList<>();

    public Locale locale = config.language;

    public PlayerData(boolean error) {
        this.error = error;
    }

    public PlayerData(String name, String uuid, String country, String country_code, String language, boolean isAdmin, int placecount, int breakcount, int killcount, int deathcount, int joincount, int kickcount, int level, int exp, int reqexp, String reqtotalexp, String firstdate, String lastdate, String lastplacename, String lastbreakname, String lastchat, String playtime, int attackclear, int pvpwincount, int pvplosecount, int pvpbreakout, int reactorcount, String bantimeset, String bantime, boolean banned, boolean translate, boolean crosschat, boolean colornick, boolean connected, String connserver, String permission, boolean mute, boolean alert, Long udid, String email, String accountid, String accountpw) {
        this.name = name;
        this.uuid = uuid;
        this.country = country;
        this.country_code = country_code;
        this.language = language;
        this.isAdmin = isAdmin;
        this.placecount = placecount;
        this.breakcount = breakcount;
        this.killcount = killcount;
        this.deathcount = deathcount;
        this.joincount = joincount;
        this.kickcount = kickcount;
        this.level = level;
        this.exp = exp;
        this.reqexp = reqexp;
        this.reqtotalexp = reqtotalexp;
        this.firstdate = firstdate;
        this.lastdate = lastdate;
        this.lastplacename = lastplacename;
        this.lastbreakname = lastbreakname;
        this.lastchat = lastchat;
        this.playtime = playtime;
        this.attackclear = attackclear;
        this.pvpwincount = pvpwincount;
        this.pvplosecount = pvplosecount;
        this.pvpbreakout = pvpbreakout;
        this.reactorcount = reactorcount;
        this.bantimeset = bantimeset;
        this.bantime = bantime;
        this.banned = banned;
        this.translate = translate;
        this.crosschat = crosschat;
        this.colornick = colornick;
        this.connected = connected;
        this.connserver = connserver;
        this.permission = permission;
        this.mute = mute;
        this.alert = alert;
        this.udid = udid;
        this.email = email;
        this.accountid = accountid;
        this.accountpw = accountpw;

        this.error = false;
        this.locale = tool.TextToLocale(country_code);
    }

    public void name(String name) {
        this.name = name;
    }

    public void uuid(String uuid) {
        this.uuid = uuid;
    }

    public void country(String country) {
        this.country = country;
    }

    public void country_code(String country_code) {
        this.country_code = country_code;
    }

    public void language(String language) {
        this.language = language;
    }

    public void admin(boolean admin) {
        isAdmin = admin;
    }

    public void placecount(int placecount) {
        this.placecount = placecount;
    }

    public void breakcount(int breakcount) {
        this.breakcount = breakcount;
    }

    public void killcount(int killcount) {
        this.killcount = killcount;
    }

    public void deathcount(int deathcount) {
        this.deathcount = deathcount;
    }

    public void joincount(int joincount) {
        this.joincount = joincount;
    }

    public void kickcount(int kickcount) {
        this.kickcount = kickcount;
    }

    public void level(int level) {
        this.level = level;
    }

    public void exp(int exp) {
        this.exp = exp;
    }

    public void reqexp(int reqexp) {
        this.reqexp = reqexp;
    }

    public void reqtotalexp(String reqtotalexp) {
        this.reqtotalexp = reqtotalexp;
    }

    public void firstdate(String firstdate) {
        this.firstdate = firstdate;
    }

    public void lastdate(String lastdate) {
        this.lastdate = lastdate;
    }

    public void lastplacename(String lastplacename) {
        this.lastplacename = lastplacename;
    }

    public void lastbreakname(String lastbreakname) {
        this.lastbreakname = lastbreakname;
    }

    public void lastchat(String lastchat) {
        this.lastchat = lastchat;
    }

    public void playtime(String playtime) {
        this.playtime = playtime;
    }

    public void attackclear(int attackclear) {
        this.attackclear = attackclear;
    }

    public void pvpwincount(int pvpwincount) {
        this.pvpwincount = pvpwincount;
    }

    public void pvplosecount(int pvplosecount) {
        this.pvplosecount = pvplosecount;
    }

    public void pvpbreakout(int pvpbreakout) {
        this.pvpbreakout = pvpbreakout;
    }

    public void reactorcount(int reactorcount) {
        this.reactorcount = reactorcount;
    }

    public void bantimeset(String bantimeset) {
        this.bantimeset = bantimeset;
    }

    public void bantime(String bantime) {
        this.bantime = bantime;
    }

    public void banned(boolean banned) {
        this.banned = banned;
    }

    public void translate(boolean translate) {
        this.translate = translate;
    }

    public void crosschat(boolean crosschat) {
        this.crosschat = crosschat;
    }

    public void colornick(boolean colornick) {
        this.colornick = colornick;
    }

    public void connected(boolean connected) {
        this.connected = connected;
    }

    public void connserver(String connserver) {
        this.connserver = connserver;
    }

    public void permission(String permission) {
        this.permission = permission;
    }

    public void mute(boolean mute) {
        this.mute = mute;
    }

    public void alert(boolean alert) {
        this.alert = alert;
    }

    public void udid(Long udid) {
        this.udid = udid;
    }

    public void email(String email) {
        this.email = email;
    }

    public void accountid(String accountid) {
        this.accountid = accountid;
    }

    public void accountpw(String accountpw) {
        this.accountpw = accountpw;
    }

    public void error(boolean error) {
        this.error = error;
    }

    public void login(boolean login) {
        isLogin = login;
    }

    public void afk(LocalTime afk) {
        this.afk = afk;
    }

    public void afk_tilex(int afk_tilex) {
        this.afk_tilex = afk_tilex;
    }

    public void afk_tiley(int afk_tiley) {
        this.afk_tiley = afk_tiley;
    }

    public void grief_build_count(int grief_build_count) {
        this.grief_build_count = grief_build_count;
    }

    public void grief_destory_count(int grief_destory_count) {
        this.grief_destory_count = grief_destory_count;
    }

    public void grief_tilelist(ArrayList<short[]> grief_tilelist) {
        this.grief_tilelist = grief_tilelist;
    }

    public void locale(Locale locale) {
        this.locale = locale;
    }
}
