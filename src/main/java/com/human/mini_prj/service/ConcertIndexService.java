package com.human.mini_prj.service;

import com.human.mini_prj.constant.IndexStatus;
import com.human.mini_prj.entity.Concert;
import com.human.mini_prj.rag.RagClient;
import com.human.mini_prj.rag.dto.IndexReqDto;
import com.human.mini_prj.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertIndexService {

    private final ConcertRepository concertRepository;
    private final RagClient ragClient;

    // 공연 단건 색인
    public void indexConcert(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 공연이 존재하지 않습니다. id=" + concertId
                ));

        // 색인 진행 상태 저장
        updateStatus(concert, IndexStatus.INDEXING, null, null);

        try {
            IndexReqDto dto = new IndexReqDto(
                    concert.getId(),
                    concert.getTitle(),
                    concert.getPrice(),
                    concert.getLocation(),
                    concert.getNotice(),
                    concert.getCasting(),
                    concert.getDetail(),
                    concert.getPriceInfo(),
                    concert.getGuide(),
                    concert.getRefund(),
                    concert.getStory(),
                    concert.getMood()
            );

            int chunks = ragClient.index(dto);

            if (chunks <= 0) {
                throw new IllegalStateException("색인된 청크가 없습니다.");
            }

            updateStatus(concert, IndexStatus.READY, null, LocalDateTime.now());

            log.info("[RAG] 공연 색인 완료: concertId={}, chunks={}",
                    concertId, chunks);

        } catch (Exception e) {
            updateStatus(concert, IndexStatus.FAILED, e.getMessage(), null);

            log.error("[RAG] 공연 색인 실패: concertId={}", concertId, e);
        }
    }


    protected void updateStatus(
            Concert concert,
            IndexStatus status,
            String error,
            LocalDateTime indexedAt
    ) {
        concert.setIndexStatus(status);
        concert.setIndexError(error);

        // 색인 성공한 경우에만 마지막 성공 시각 갱신
        if (status == IndexStatus.READY) {
            concert.setIndexedAt(indexedAt);
        }

        concertRepository.save(concert);
    }
    // 전체 공연 재색인
    public int reindexAllConcerts() {
        List<Concert> concerts = concertRepository.findAll();

        int successCount = 0;

        for (Concert concert : concerts) {
            indexConcert(concert.getId());

            // 색인 성공 여부 확인
            Concert updated = concertRepository.findById(concert.getId())
                    .orElseThrow();

            if (updated.getIndexStatus() == IndexStatus.READY) {
                successCount++;
            }
        }
        log.info("[RAG] 전체 공연 재색인 완료: 전체={}, 성공={}, 실패={}",
                concerts.size(), successCount, concerts.size() - successCount);

        return successCount;
    }
}