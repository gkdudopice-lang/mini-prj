package com.human.mini_prj.repository;

import com.human.mini_prj.constant.IndexStatus;
import com.human.mini_prj.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    // 색인 상태에 해당하는 공연 ID 목록 조회
    List<Concert> findByIndexStatus(IndexStatus indexStatus);
}