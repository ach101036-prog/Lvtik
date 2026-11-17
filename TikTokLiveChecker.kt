import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class TikTokLiveChecker {
    private val client = OkHttpClient()

    fun checkLiveStatus(username: String): Result<String?> {
        val cleanUser = username.trim().removePrefix("@")
        // TikTok's internal web API endpoint
        val url = "https://www.tiktok.com/api-live/user/room/?aid=1988&app_name=tiktok_web&device_platform=web_pc&uniqueId=$cleanUser"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return Result.failure(Exception("HTTP ${response.code}"))
            
            val json = JSONObject(response.body?.string() ?: "")
            val statusCode = json.optInt("statusCode", -1)
            
            if (statusCode != 0) {
                // 19881007 usually means user not found
                return Result.failure(Exception("User not found or API error: $statusCode"))
            }

            val data = json.optJSONObject("data")
            val user = data?.optJSONObject("user")
            val liveRoom = data?.optJSONObject("liveRoom")
            
            val liveStatus = liveRoom?.optInt("status", 0) ?: 0
            val userStatus = user?.optInt("status", 0) ?: 0

            // Status 2 means the user is currently live
            if (liveStatus == 2 || userStatus == 2) {
                // In a full implementation, you would parse the HLS stream URL from the 'liveRoom' object here.
                // For this example, we return a success flag to trigger the recorder.
                Result.success("LIVE") 
            } else {
                Result.success(null) // User is not live
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
