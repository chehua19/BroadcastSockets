package sockets;

import content.KumoConf;
import org.json.JSONException;
import org.json.JSONObject;
import utils.Request;

import java.io.IOException;

public class KumoSocket {
    private String ip;

    public KumoSocket(String ip){
        this.ip = ip;
    }

    public void setup() throws IOException {
        String name = Request.doGet(ip + "/options?action=get&alt=text-plain&paramid=eParamID_SysName");
        KumoConf.NAME = name;
        int numOfSignals = Integer.parseInt(name.substring(name.indexOf(" ")+1, name.indexOf(" ")+3));

        for (int i = 1; i <= numOfSignals; i++) {
            KumoConf.SOURCE.add(getSignalName(i, true));
            KumoConf.DESTINATION.add(getSignalName(i, false));
        }
    }

    public void changeDestination(int destination, int source) throws IOException {
        Request.doGet(ip + "/config?action=set&alt=json&paramid=eParamID_XPT_Destination" + (destination + 1) + "_Status&value=" + (source + 1));
    }

    private String getSignalName(int numOfSource, boolean isSource) throws IOException{
        StringBuilder signalName = new StringBuilder();
        for (int j = 1; j <= 2; j++) {
            String jsonString = Request.doGet(ip + "/config?action=get&alt=json&paramid=eParamID_XPT_" + (isSource ? "Source" : "Destination") + numOfSource + "_Line_" + j);
            try {
                JSONObject json = new JSONObject(jsonString);
                signalName.append(json.get("value")).append(j == 1 ? " " : "");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return signalName.toString().trim();
    }

    public String getIp() {
        return ip;
    }
}