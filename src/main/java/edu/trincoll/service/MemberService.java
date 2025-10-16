package edu.trincoll.service;

import edu.trincoll.model.Member;
import edu.trincoll.repository.MemberRepository;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with email: " + email));
    }

    public void incrementCheckoutCount(Member member) {
        member.setBooksCheckedOut(member.getBooksCheckedOut() + 1);
        memberRepository.save(member);
    }

    public void decrementCheckoutCount(Member member) {
        member.setBooksCheckedOut(member.getBooksCheckedOut() - 1);
        memberRepository.save(member);
    }
}
