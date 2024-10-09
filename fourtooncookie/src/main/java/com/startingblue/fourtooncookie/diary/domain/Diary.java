package com.startingblue.fourtooncookie.diary.domain;

import com.startingblue.fourtooncookie.character.domain.Character;
import com.startingblue.fourtooncookie.diary.domain.converter.DiaryPaintingImageGenerationStatusListConverter;
import com.startingblue.fourtooncookie.global.domain.BaseEntity;
import com.startingblue.fourtooncookie.global.converter.jpa.UrlListToStringConverter;
import jakarta.persistence.*;
import jakarta.validation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class Diary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long id;

    @NotBlank(message = "일기 내용은 필수 입니다.")
    @Size(min = 1, max = 1000, message = "일기 내용은 1자 이상 1000자 이내여야 합니다.")
    private String content;

    private boolean isFavorite;

    @NotNull(message = "일기 날짜는 필수 입니다.")
    @Column(updatable = false)
    private LocalDate diaryDate;

    @Size(max = 4, message = "일기 그림은 최대 4개 입니다.")
    @Convert(converter = UrlListToStringConverter.class)
    @Builder.Default
    private List<URL> paintingImageUrls = new ArrayList<>();

    @NotNull(message = "일기에 그려질 캐릭터는 필수 입니다.")
    @ManyToOne(fetch = FetchType.LAZY)
    private Character character;

    @NotNull(message = "멤버 아이디는 필수 입니다.")
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "일기 상태는 필수 입니다.")
    @Builder.Default
    private DiaryStatus status = DiaryStatus.IN_PROGRESS;

    @Convert(converter = DiaryPaintingImageGenerationStatusListConverter.class)
    @Builder.Default
    private List<DiaryPaintingImageGenerationStatus> paintingImageGenerationStatuses = new ArrayList<>();

    public static DiaryBuilder builder() {
        return new CustomDiaryBuilder();
    }

    public void update(String content,
                       Character character) {
        this.content = content;
        this.character = character;
        this.status = DiaryStatus.IN_PROGRESS;
        paintingImageGenerationStatuses = new ArrayList<>(
                Collections.nCopies(paintingImageGenerationStatuses.size(), DiaryPaintingImageGenerationStatus.GENERATING)
        );
        validate();
    }

    public void updateFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public void updatePaintingImageUrls(List<URL> paintingImageUrls) {
        this.paintingImageUrls = new ArrayList<>(paintingImageUrls);
    }

    public void updatePaintingImageGenerationStatus(int index, boolean isImageGenerationSuccess) {
        paintingImageGenerationStatuses = new ArrayList<>(paintingImageGenerationStatuses);
        paintingImageGenerationStatuses.set(index,
                isImageGenerationSuccess ? DiaryPaintingImageGenerationStatus.SUCCESS : DiaryPaintingImageGenerationStatus.FAILURE);
    }


    public void updateDiaryStatus(DiaryStatus status) {
        this.status = status;
    }
    
    public boolean isOwner(UUID memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isImageGenerationComplete() {
        return paintingImageGenerationStatuses.stream()
                .allMatch(DiaryPaintingImageGenerationStatus.SUCCESS::equals);
    }

    private void validate() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<Diary>> violations = validator.validate(this);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private static class CustomDiaryBuilder extends DiaryBuilder {
        @Override
        public Diary build() {
            Diary diary = super.build();
            diary.validate();
            return diary;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Diary diary = (Diary) o;
        return isFavorite == diary.isFavorite &&
                Objects.equals(id, diary.id) &&
                Objects.equals(content, diary.content) &&
                Objects.equals(diaryDate, diary.diaryDate) &&
                Objects.equals(paintingImageUrls, diary.paintingImageUrls) &&
                Objects.equals(paintingImageGenerationStatuses, diary.paintingImageGenerationStatuses) &&
                Objects.equals(character, diary.character) &&
                Objects.equals(memberId, diary.memberId) &&
                Objects.equals(getCreatedDateTime(), diary.getCreatedDateTime()) &&
                Objects.equals(getModifiedDateTime(), diary.getModifiedDateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, isFavorite, diaryDate, paintingImageUrls, character, memberId, getCreatedDateTime(), getModifiedDateTime());
    }
}
