package com.example.guru2_android_team04_android.util

import org.json.JSONArray

// JsonMini : 간단한 List<String> - JSON 문자열 변환을 담당하는 경량 유틸리티
// 용도:
// - SQLite에는 List<String> 타입을 직접 저장할 수 없기 때문에, 태그(tags), 행동(actions), 해시태그(hashtags) 등을 JSON 문자열로 변환하여 저장한다.
// - DB에서 읽어온 JSON 문자열을 다시 List<String> 형태로 복원할 때 사용한다.
object JsonMini {
    // List<String> -> JSON 문자열로 변환
    // 처리 방식:
    // - org.json.JSONArray를 사용해 문자열 배열을 구성한다.
    // - JSONArray.toString() 결과를 그대로 DB에 저장한다.
    fun listToJson(list: List<String>): String {
        val arr = JSONArray()
        for (s in list) arr.put(s)
        return arr.toString()
    }

    // JSON 문자열 -> List<String>으로 변환한다.
    // 반환:
    // - JSON 배열에 들어 있는 문자열을 순서 그대로 List로 반환한다.
    // 예외처리):
    // - json이 "[]" 인 경우 길이가 0인 JSONArray가 생성되어 자연스럽게 빈 리스트가 반환된다.
    // - 잘못된 JSON 문자열이 들어오면 JSONArray 생성 시 예외가 발생할 수 있으며, 이 경우 호출부에서 예외를 처리하도록 한다.
    fun jsonToList(json: String): List<String> {
        val arr = JSONArray(json)
        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) out.add(arr.getString(i))
        return out
    }
}
