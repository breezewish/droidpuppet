package org.breeswish.droidpuppet.media

import android.media.MediaCodecList
import android.media.MediaCodecInfo
import android.media.MediaFormat

class Codecs {
    companion object {
        private val VIDEO_FORMATS = arrayOf(
                MediaFormat.MIMETYPE_VIDEO_AVC,     // H.264
                MediaFormat.MIMETYPE_VIDEO_HEVC,    // H.265
                MediaFormat.MIMETYPE_VIDEO_VP8,     // VP8
                MediaFormat.MIMETYPE_VIDEO_VP9      // VP9
        )

        /**
         * Get all available video encoders. Returns `{ format_in_mime => [encoder_name] }`.
         */
        fun getVideoEncoders(): HashMap<String, ArrayList<MediaCodecInfo>> {
            val ret = HashMap<String, ArrayList<MediaCodecInfo>>()
            for (format in VIDEO_FORMATS) {
                val encoders = getEncoders(format)
                if (encoders.count() > 0) {
                    ret[format] = encoders
                }
            }
            return ret
        }

        /**
         * Get encoders for a format specified by the MIME type.
         */
        private fun getEncoders(format: String): ArrayList<MediaCodecInfo> {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val infoList = ArrayList<MediaCodecInfo>()
            for (info in codecList.codecInfos) {
                if (!info.isEncoder) {
                    continue
                }
                try {
                    if (info.getCapabilitiesForType(format) != null) {
                        infoList.add(info)
                    }
                } catch (e: IllegalArgumentException) {
                }
            }
            return infoList
        }
    }
}
