package com.river.detection.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/api/map")
@CrossOrigin
public class MapProxyController {

    // 高德 key 可迁移到配置；此处为演示
    private static final String AMAP_KEY = "fd57f50c370599504187ca0c4a673328";

    private static final String[] AMAP_TEMPLATES = new String[]{
            "https://webrd0%s.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=7&x=%d&y=%d&z=%d&key=" + AMAP_KEY,
            "https://webst0%s.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=7&x=%d&y=%d&z=%d&key=" + AMAP_KEY,
            "https://wprd0%s.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=7&x=%d&y=%d&z=%d&key=" + AMAP_KEY
    };

    private static final String OSM_TEMPLATE = "https://%s.tile.openstreetmap.org/%d/%d/%d.png";

    private byte[] httpGetBytes(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(6000);
        conn.setRequestProperty("User-Agent", "pfw-map-proxy/1.0");
        conn.connect();
        int code = conn.getResponseCode();
        if (code == 200) {
            try (InputStream is = conn.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
                return bos.toByteArray();
            }
        }
        throw new IOException("Upstream status: " + code);
    }

    private String httpGetString(String urlStr) throws IOException {
        return new String(httpGetBytes(urlStr), StandardCharsets.UTF_8);
    }

    @GetMapping(value = "/amap/{z}/{x}/{y}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> proxyAmap(@PathVariable int z, @PathVariable int x, @PathVariable int y) {
        String[] sub = new String[]{"1","2","3","4"};
        // 依次尝试不同域名与子域
        for (String tpl : AMAP_TEMPLATES) {
            for (String s : sub) {
                String url = String.format(tpl, s, x, y, z);
                try {
                    byte[] bytes = httpGetBytes(url);
                    return png(bytes, 3600);
                } catch (Exception ignored) {}
            }
        }
        // 高德失败，尝试 OSM
        for (String s : new String[]{"a","b","c"}) {
            String url = String.format(OSM_TEMPLATE, s, z, x, y);
            try {
                byte[] bytes = httpGetBytes(url);
                return png(bytes, 600);
            } catch (Exception ignored) {}
        }
        // 全部失败返回占位图，避免前端报错刷屏
        return png(blankTile(), 300);
    }

    // 地名转坐标（高德地理编码）
    @GetMapping(value = "/geocode", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> geocode(@RequestParam("address") String address,
                                          @RequestParam(value = "city", required = false) String city) {
        try {
            String a = URLEncoder.encode(address, "UTF-8");
            String c = city != null ? URLEncoder.encode(city, "UTF-8") : null;
            String url = "https://restapi.amap.com/v3/geocode/geo?key=" + AMAP_KEY + "&address=" + a + (c != null ? ("&city=" + c) : "");
            String json = httpGetString(url);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":0,\"info\":\"geocode error: " + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    // 路径规划（驾车）
    @GetMapping(value = "/direction", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> direction(@RequestParam("origin") String origin,
                                            @RequestParam("destination") String destination) {
        try {
            // origin/destination 形如 lng,lat（GCJ-02）
            String url = "https://restapi.amap.com/v3/direction/driving?key=" + AMAP_KEY + "&origin=" + origin + "&destination=" + destination + "&strategy=0";
            String json = httpGetString(url);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                    .body("{\"status\":0,\"info\":\"direction error: " + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    private ResponseEntity<byte[]> png(byte[] body, int maxAgeSeconds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.maxAge(Duration.ofSeconds(maxAgeSeconds)));
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private byte[] blankTile() {
        try {
            BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(240, 243, 246));
            g.fillRect(0,0,256,256);
            g.setColor(new Color(220, 225, 230));
            for (int i=0;i<=256;i+=32) {
                g.drawLine(i,0,i,256);
                g.drawLine(0,i,256,i);
            }
            g.dispose();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
