package com.startingblue.fourtooncookie.member.service;

import com.startingblue.fourtooncookie.member.domain.Member;
import com.startingblue.fourtooncookie.member.domain.MemberRepository;
import com.startingblue.fourtooncookie.member.domain.Role;
import com.startingblue.fourtooncookie.member.dto.request.MemberSaveRequest;
import com.startingblue.fourtooncookie.member.exception.MemberDuplicateException;
import com.startingblue.fourtooncookie.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Transactional
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public void save(UUID memberId, MemberSaveRequest memberSaveRequest) {
        if (verifyMemberExists(memberId)) {
            throw new MemberDuplicateException("Member with id " + memberId + " already exists");
        };

        Member member = Member.builder()
                .id(memberId)
                .name(memberSaveRequest.name())
                .birth(memberSaveRequest.birth())
                .gender(memberSaveRequest.gender())
                .role(Role.MEMBER)
                .build();
        memberRepository.save(member);
    }

    public Member readById(UUID memberId) {
        return  memberRepository.findById(memberId).orElseThrow(() -> new MemberNotFoundException("member not found"));
    }

    public void hardDeleteById(UUID memberId) {
        memberRepository.deleteById(memberId);
    }

    public void softDeleteById(UUID memberId) {
        softDeleteById(memberId, LocalDateTime.now());
    }

    public void softDeleteById(UUID memberId, LocalDateTime current) {
        Member foundMember = readById(memberId);
        foundMember.softDelete(current);
        memberRepository.save(foundMember);
    }

    public boolean verifyMemberExists(UUID memberId) {
        return memberRepository.existsById(memberId);
    }

    public boolean verifyMemberSignUp(UUID memberId) {
        Member member = readById(memberId);
        return member != null && member.getName() != null && !member.getName().isEmpty();
    }

    public boolean verifyMemberAdmin(UUID memberId) {
        Member member = readById(memberId);
        return member.isAdmin();
    }

}
