package com.startingblue.fourtooncookie.diary.domain;

import com.startingblue.fourtooncookie.character.domain.Character;
import com.startingblue.fourtooncookie.DiaryHashtag;
import com.startingblue.fourtooncookie.hashtag.domain.Hashtag;
import com.startingblue.fourtooncookie.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Entity
@Slf4j
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private Character character;

    private String content;

    private String thumbnailUrl;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryHashtag> hashtags = new ArrayList<>();

    public Diary(Member member, Character character, String content, String thumbnailUrl) {
        this.member = member;
        this.character = character;
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void addHashtag(Hashtag hashtag) {
        DiaryHashtag diaryHashtag = new DiaryHashtag(this, hashtag);
        hashtags.add(diaryHashtag);
    }

    public void removeHashtag(Hashtag hashtag) {
        hashtags.removeIf(diaryHashtag -> diaryHashtag.getHashtag().equals(hashtag));
    }
}
