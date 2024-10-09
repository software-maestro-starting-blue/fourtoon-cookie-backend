package com.startingblue.fourtooncookie.diary.domain;

import com.startingblue.fourtooncookie.artwork.domain.Artwork;
import com.startingblue.fourtooncookie.artwork.domain.ArtworkRepository;
import com.startingblue.fourtooncookie.character.domain.Character;
import com.startingblue.fourtooncookie.character.domain.CharacterRepository;
import com.startingblue.fourtooncookie.character.domain.CharacterVisionType;
import com.startingblue.fourtooncookie.global.domain.PaymentType;
import com.startingblue.fourtooncookie.member.domain.Gender;
import com.startingblue.fourtooncookie.member.domain.Member;
import com.startingblue.fourtooncookie.member.domain.MemberRepository;
import com.startingblue.fourtooncookie.member.domain.Role;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DiaryTest {

    private Character character;
    private Member member;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CharacterRepository characterRepository;

    private URL validUrl;
    private UUID validMemberId;

    @BeforeEach
    void setUp() throws MalformedURLException {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

        member = Member.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .birth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .role(Role.MEMBER)
                .build();
        member = memberRepository.save(member);
        validUrl = new URL("http://example.com/image.png");
        validMemberId = member.getId();

        Artwork artwork;
        try {
            artwork = new Artwork("Test Artwork", new URL("https://test.png"));
            artworkRepository.save(artwork);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        character = Character.builder()
                .characterVisionType(CharacterVisionType.DALL_E_3)
                .paymentType(PaymentType.FREE)
                .artwork(artwork)
                .name("CharacterName")
                .selectionThumbnailUrl(createURL("https://character.png"))
                .basePrompt("basePrompt")
                .build();
        character = characterRepository.save(character);
    }

    private URL createURL(String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("유효한 Diary 객체 생성")
    public void validDiaryCreation() {
        Diary diary = Diary.builder()
                .content("Valid content")
                .diaryDate(LocalDate.now())
                .paintingImageUrls(List.of(validUrl))
                .character(character)
                .memberId(validMemberId)
                .build();

        assertThat(diary).isNotNull();
        assertThat(diary.getContent()).isEqualTo("Valid content");
        assertThat(diary.getDiaryDate()).isEqualTo(LocalDate.now());
        assertThat(diary.getPaintingImageUrls()).containsExactly(validUrl);
        assertThat(diary.getCharacter()).isEqualTo(character);
        assertThat(diary.getMemberId()).isEqualTo(validMemberId);
    }

    @Test
    @DisplayName("내용이 빈 문자열일 때 Diary 객체 생성 시 ConstraintViolationException")
    public void testInvalidDiaryCreation_ContentBlank() {
        assertThatThrownBy(() -> {
            Diary.builder()
                    .content("")
                    .diaryDate(LocalDate.now())
                    .paintingImageUrls(List.of(validUrl))
                    .character(character)
                    .memberId(validMemberId)
                    .build();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("일기 내용은 필수 입니다.");
    }

    @Test
    @DisplayName("날짜가 null일 때 Diary 객체 생성 시 ConstraintViolationException")
    public void testInvalidDiaryCreation_DateNull() {
        assertThatThrownBy(() -> {
            Diary.builder()
                    .content("Valid content")
                    .diaryDate(null)
                    .paintingImageUrls(List.of(validUrl))
                    .character(character)
                    .memberId(validMemberId)
                    .build();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("일기 날짜는 필수 입니다.");
    }

    @Test
    @DisplayName("캐릭터가 null일 때 Diary 객체 생성 시 ConstraintViolationException")
    public void testInvalidDiaryCreation_CharacterNull() {
        assertThatThrownBy(() -> {
            Diary.builder()
                    .content("Valid content")
                    .diaryDate(LocalDate.now())
                    .paintingImageUrls(List.of(validUrl))
                    .character(null)
                    .memberId(validMemberId)
                    .build();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("일기에 그려질 캐릭터는 필수 입니다.");
    }

    @Test
    @DisplayName("멤버 아이디가 null일 때 Diary 객체 생성 시 ConstraintViolationException")
    public void testInvalidDiaryCreation_MemberIdNull() {
        assertThatThrownBy(() -> {
            Diary.builder()
                    .content("Valid content")
                    .diaryDate(LocalDate.now())
                    .paintingImageUrls(List.of(validUrl))
                    .character(character)
                    .memberId(null)
                    .build();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("멤버 아이디는 필수 입니다.");
    }

    @Test
    @DisplayName("Diary 객체 업데이트 성공")
    public void testUpdateDiary() {
        Diary diary = Diary.builder()
                .content("Old content")
                .diaryDate(LocalDate.now())
                .paintingImageUrls(List.of(validUrl))
                .character(character)
                .memberId(validMemberId)
                .build();

        String newContent = "New content";
        diary.update(newContent, character);

        assertThat(diary.getContent()).isEqualTo(newContent);
        assertThat(diary.getCharacter()).isEqualTo(character);
    }

    @Test
    @DisplayName("Diary 업데이트 시 내용이 빈 문자열일 때 ConstraintViolationException")
    public void testUpdateDiary_InvalidContent() {
        Diary diary = Diary.builder()
                .content("Valid content")
                .diaryDate(LocalDate.now())
                .paintingImageUrls(List.of(validUrl))
                .character(character)
                .memberId(validMemberId)
                .build();

        assertThatThrownBy(() -> {
            diary.update("", character);
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("일기 내용은 필수 입니다.");
    }

    @Test
    @DisplayName("Diary 업데이트 시 캐릭터가 null일 때 ConstraintViolationException")
    public void testUpdateDiary_InvalidCharacter() {
        Diary diary = Diary.builder()
                .content("Valid content")
                .diaryDate(LocalDate.now())
                .paintingImageUrls(List.of(validUrl))
                .character(character)
                .memberId(validMemberId)
                .build();

        assertThatThrownBy(() -> {
            diary.update("Updated content", null);
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("일기에 그려질 캐릭터는 필수 입니다.");
    }

    @Test
    @DisplayName("일기 내용이 1000자를 초과할 때 Diary 객체 생성 시 ConstraintViolationException")
    public void testInvalidDiaryCreation_ContentTooLong() {
        String longContent = "a".repeat(1001);

        assertThatThrownBy(() -> {
            Diary.builder()
                    .content(longContent)
                    .diaryDate(LocalDate.now())
                    .paintingImageUrls(List.of(validUrl))
                    .character(character)
                    .memberId(validMemberId)
                    .build();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("일기 내용은 1자 이상 1000자 이내여야 합니다.");
    }

    @Test
    @DisplayName("일기 그림 URL 목록 업데이트 테스트")
    public void testUpdatePaintingImageUrls() throws MalformedURLException {
        Diary diary = Diary.builder()
                .content("Valid content")
                .diaryDate(LocalDate.now())
                .paintingImageUrls(List.of(validUrl))
                .character(character)
                .memberId(validMemberId)
                .build();

        URL newUrl1 = new URL("http://example.com/newimage1.png");
        URL newUrl2 = new URL("http://example.com/newimage2.png");

        diary.updatePaintingImageUrls(List.of(newUrl1, newUrl2));

        assertThat(diary.getPaintingImageUrls()).containsExactly(newUrl1, newUrl2);
    }


    @Test
    @DisplayName("일기 그림 URL 목록이 5개 이상일 때 Diary 객체 생성 시 ConstraintViolationException")
    public void testInvalidDiaryCreation_PaintingImageUrlsTooMany() {
        assertThatThrownBy(() -> {
            Diary.builder()
                    .content("Valid content")
                    .diaryDate(LocalDate.now())
                    .paintingImageUrls(List.of(validUrl, validUrl, validUrl, validUrl, validUrl))
                    .character(character)
                    .memberId(validMemberId)
                    .build();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("일기 그림은 최대 4개 입니다.");
    }

    @Test
    @DisplayName("일기 즐겨찾기 업데이트 테스트")
    public void testUpdateFavorite() {
        Diary diary = Diary.builder()
                .content("Valid content")
                .diaryDate(LocalDate.now())
                .paintingImageUrls(List.of(validUrl))
                .character(character)
                .memberId(validMemberId)
                .build();

        diary.updateFavorite(true);

        assertThat(diary.isFavorite()).isTrue();

        diary.updateFavorite(false);

        assertThat(diary.isFavorite()).isFalse();
    }


    @Test
    @DisplayName("멤버가 소유자일 때 테스트")
    public void testIsOwner_ValidOwner() {
        Diary diary = Diary.builder()
                .content("Valid content")
                .diaryDate(LocalDate.now())
                .paintingImageUrls(List.of(validUrl))
                .character(character)
                .memberId(validMemberId)
                .build();

        assertThat(diary.isOwner(validMemberId)).isTrue();
    }

    @Test
    @DisplayName("멤버가 소유자가 아닐 때 테스트")
    public void testIsOwner_InvalidOwner() {
        Diary diary = Diary.builder()
                .content("Valid content")
                .diaryDate(LocalDate.now())
                .paintingImageUrls(List.of(validUrl))
                .character(character)
                .memberId(validMemberId)
                .build();

        UUID invalidMemberId = UUID.randomUUID();
        assertThat(diary.getId()).isNull();
        assertThat(diary.isOwner(invalidMemberId)).isFalse();
    }
}
