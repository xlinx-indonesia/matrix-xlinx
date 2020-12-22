/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.ui.views

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.FileAsyncHttpResponseHandler
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import im.vector.app.core.qrcode.toBitMatrix
import im.vector.app.core.qrcode.toBitmap
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileInputStream

class QrCodeImageView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var data: String? = null

    init {
        setBackgroundColor(Color.WHITE)
    }

    fun setData(data: String) {
        this.data = data

        render()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        render()
    }

    private fun render() {
//        data
//                ?.takeIf { height > 0 }
//                ?.let {
//                    val bitmap = it.toBitMatrix(height).toBitmap()
//                    post { setImageBitmap(bitmap) }
//                }
        data?.let { createXlinxQR(it) }
    }

    private fun createXlinxQR(qrData: String) {
        val client = AsyncHttpClient()

        client.addHeader("content-type", "application/json")
        client.addHeader("x-rapidapi-key", "0dc323c809msh8c0e6a30eae3f54p120c73jsnbfef66dfde9d")
        client.addHeader("x-rapidapi-host", "qrcode-monkey.p.rapidapi.com")

        val jsonString = "{\n    \"body\": \"leaf\",\n        \"eye\": \"frame2\",\n        \"eyeBall\": \"ball17\",\n        \"erf1\": [],\n        \"erf2\": [fh],\n        \"erf3\": [fv],\n        \"brf1\": [],\n        \"brf2\": [fh],\n        \"brf3\": [fv],\n        \"bodyColor\": \"#15161D\",\n        \"bgColor\": \"#FFFFFF\",\n        \"eye1Color\": \"#2B79E5\",\n        \"eye2Color\": \"#2B79E5\",\n        \"eye3Color\": \"#2B79E5\",\n        \"eyeBall1Color\": \"#8DC21F\",\n        \"eyeBall2Color\": \"#8DC21F\",\n        \"eyeBall3Color\": \"#8DC21F\",\n        \"gradientColor1\": \"\",\n        \"gradientColor2\": \"\",\n        \"gradientType\": \"\",\n        \"gradientOnEyes\": false,\n        \"logo\": \"https://raw.githubusercontent.com/ngodn/fileassets/main/ic_xlinx_qr_onwhite.png\"\n    }"
        var jsonData: JSONObject? = null
        try {
            jsonData = JSONObject(jsonString)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val params = RequestParams()
        params.put("data", qrData)
        params.put("config", jsonData)
        params.put("size", 512)
        params.put("download", false)
        params.put("file", "png")

        client["https://rapidapi.p.rapidapi.com/qr/custom", params, object : FileAsyncHttpResponseHandler(context) {
            override fun onFailure(statusCode: Int, headers: Array<Header>, throwable: Throwable, file: File) {
                Timber.e("fileqr : %s", throwable.message)
            }
            override fun onSuccess(statusCode: Int, headers: Array<Header>, file: File) {
                Timber.i("fileqr : $statusCode")
                file
                        .takeIf { height > 0 }
                        ?.let {
                            val fileInputStream = FileInputStream(file)
                            post { setImageBitmap(BitmapFactory.decodeStream(fileInputStream)) }
                        }
            }
        }]
    }
}
