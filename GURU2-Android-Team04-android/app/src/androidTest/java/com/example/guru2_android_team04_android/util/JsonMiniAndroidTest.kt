package com.example.guru2_android_team04_android.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// JsonMiniAndroidTest : JsonMini 유틸의 직렬화/역직렬화가 서로 역연산인지 검증하는 테스트
// 용도:
// - DiaryEntry.tags 처럼 List<String> 데이터를 SQLite에 저장할 때 JSON 문자열로 변환해서 넣는다.
// - 저장 후 다시 읽어올 때 JSON -> List<String>으로 복원되는데, 이 과정에서 값/순서가 깨지지 않는지 확인한다.
@RunWith(AndroidJUnit4::class)
class JsonMiniAndroidTest {

    @Test
    fun list_to_json_and_back() {
        // 테스트 입력: 사용자가 선택할 수 있는 태그 예시 3개
        val src = listOf("불안", "평온", "기쁨")

        // listToJson(): List<String> -> JSON 배열 문자열로 변환
        val json = JsonMini.listToJson(src)

        // jsonToList(): JSON 배열 문자열 -> List<String>으로 복원
        // - DB에서 tags_json을 읽을 때 쓰는 역변환 과정과 동일하다.
        val back = JsonMini.jsonToList(json)

        // 원본 리스트와 복원된 리스트가 완전히 같아야 한다.
        // - 값이 같고, 순서도 같아야 한다.
        assertEquals(src, back)
    }
}
