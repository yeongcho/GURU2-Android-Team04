package com.example.guru2_android_team04_android.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.guru2_android_team04_android.R
import com.example.guru2_android_team04_android.databinding.FragmentDiaryDetailBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class DiaryDetailFragment : Fragment() {
    private var _binding: FragmentDiaryDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 공통 하단바 '캘린더' 아이콘 활성화 상태 유지
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav?.selectedItemId = R.id.navigation_calendar

        // 2. 전달받은 텍스트 데이터 뷰에 세팅

        // [일기 본문 카드]
        binding.tvDiaryContent.text = """
            오랜만에 고등학교 때 친구들을 만났다. 거창한 맛집을 찾아다니거나 핫플레이스를 간 것도 아니다. 그냥 동네 단골 카페 구석 자리에 앉아 각자 시킨 커피를 마시며 3시간 동안 수다만 떨었다. 특별한 이야기도 아니었다. 요즘 회사가 얼마나 바쁜지, 지난 주말에 본 드라마가 얼마나 재밌었는지 같은 시시콜콜한 이야기들. 그런데 그 별거 아닌 대화 속에 섞인 웃음소리가 참 좋았다. 내가 어떤 말을 해도 오해 없이 들어줄 거라는 믿음이 있어서인지, 굳이 멋진 사람처럼 보이려 애쓰지 않아도 돼서 마음이 정말 편안했다. 해가 질 무렵 헤어져서 집에 돌아오는데, 텅 비어있던 마음 한구석이 따뜻하게 채워진 기분이 들었다. 역시 나를 웃게 만드는 건 대단한 성취가 아니라, 내 편이 되어주는 사람들과 보내는 이런 소소한 시간들이다. 덕분에 이번 주는 아주 잘 보낼 수 있을 것 같다.
        """.trimIndent()

        // [오늘의 미션 탭] - ★ 이 코드가 추가되어야 배경 상자가 나타납니다.
        binding.tvMissionText.text = "오늘의 미션: 걱정 스위치 끄고 푹 잠들기"

        // [마음지기의 분석 카드]
        binding.tvDiaryAnalysis.text = """
            잘하고 싶은 마음은 굴뚝같은데 진도가 나가지 않을 때의 그 막막함, 누구나 겪을 수 있는 감정입니다. 뇌과학적으로도 스트레스가 극에 달했을 때는 억지로 정보를 주입하기보다 잠을 자는 것이 기억 정리에 훨씬 효과적이에요. 닉네임님은 그 한계점에서 '전략적 후퇴' 라는 최선의 답을 스스로 잘 찾아내셨습니다. 오늘의 후회는 내일 새벽의 폭발적인 집중력을 위한 도움닫기가 될 겁니다. 지금은 아무 걱정 말고 뇌를 완전히 쉬게 해주세요. 내일의 당신은 생각보다 훨씬 강하니까요.
        """.trimIndent()

        // [오늘의 실천안 카드]
        binding.tvActionPlan.text = """
            1. 내일 아침 망설임 없이 시작할 수 있도록, 공부할 페이지를 미리 활짝 펼쳐두세요. 
            2. 시원한 물 한 잔으로 불안을 씻어내고, 스마트폰 전원을 꺼서 뇌에게 온전한 쉼을 선물하세요. 
            3. 머릿속을 맴도는 내일의 할일들은 플래너에 모두 털어놓고, 가벼운 마음으로 잠드세요.
        """.trimIndent()

        // 3. 버튼 리스너 설정
        binding.btnPrevDay.setOnClickListener { /* 하루 전 로직 */ }
        binding.btnNextDay.setOnClickListener { /* 하루 후 로직 */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}