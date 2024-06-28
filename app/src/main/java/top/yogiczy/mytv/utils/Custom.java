package top.yogiczy.mytv.utils;
import java.util.Base64;

public class Custom {

    // Method to decode a Base64 string
    public static String decodeBase64(String input) {
        byte[] decodedBytes = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            decodedBytes = Base64.getDecoder().decode(input);
        } else {
            return "插兜的干货仓库";
        }
        return new String(decodedBytes);
    }

    // Test the encoding and decoding methods
    public static String getWechatName() {
        // 插兜的干货仓库
        String encodedString = "5o+S5YWc55qE5bmy6LSn5LuT5bqT";
        return decodeBase64(encodedString);
    }
}