
package com.human.mini_prj.repository;

import com.human.mini_prj.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 예매 정보를 DB에 저장하고 조회하는 Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 특정 회원의 예매 내역을 최신순으로 조회
    List<Reservation> findByMember_IdOrderByReservedAtDesc(Long memberId);

}
