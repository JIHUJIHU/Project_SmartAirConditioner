package com.smart.airconditioner.network;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.smart.airconditioner.MainActivity;
import com.smart.airconditioner.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class DustInfo {

    private Context context;
    private final String API_ID;

    public DustInfo(Context context) {
        this.context = context;
        API_ID = context.getString(R.string.dust_id);
    }

    public void getCurrentDust() {
        DustTask task = new DustTask();
        task.execute();
    }

    private class DustTask extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... sId) {
            String result = null;
            try {
                URL url = new URL("http://openapi.airkorea.or.kr/openapi/services/rest/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty?serviceKey=" +
                        API_ID
                        + "&numOfRows=1&pageSize=1&pageNo=1&startPage=1&stationName=%EC%9A%A9%EC%95%94%EB%8F%99&dataTerm=DAILY&ver=1.3");
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new InputSource(url.openStream()));
                doc.getDocumentElement().normalize();

                NodeList nodeList = doc.getElementsByTagName("item");

                Node node = nodeList.item(0);

                Element fstElmnt = (Element) node;
                NodeList nameList = fstElmnt.getElementsByTagName("pm10Value");
                Element nameElement = (Element) nameList.item(0);
                nameList = nameElement.getChildNodes();
                result = nameList.item(0).getNodeValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("DUST", "EXECUTE");
            ((MainActivity) context).notifyDustChange(result);
        }
    }
}
