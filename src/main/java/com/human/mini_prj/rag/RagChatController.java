package com.human.mini_prj.rag;

import com.human.mini_prj.rag.dto.IndexReqDto;
import com.human.mini_prj.rag.dto.QueryResDto;
import com.human.mini_prj.rag.dto.RagChatReqDto;
import com.human.mini_prj.service.ConcertIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagChatController {
    private final RagChatService ragChatService;
    private final ConcertIndexService concertIndexService;
    // AI 질문
    @PostMapping("/chat")
    public ResponseEntity<QueryResDto> chat (@RequestBody RagChatReqDto req) {
        return ResponseEntity.ok(ragChatService.ask(req));
    }
    // React 에 저장된 전체 공연 색인
    @PostMapping("/index")
    public ResponseEntity<String> indexAllConcerts(
            @RequestBody List<IndexReqDto> concerts) {

        int totalChunks = ragChatService.indexAllConcerts(concerts);

        return ResponseEntity.ok(
                "공연 전체 색인 완료: 총" + totalChunks +"개 청크"
        );
    }
    // 공연 단건 재색인
    @PostMapping("/reindex/{concertId}")
    public  ResponseEntity<String> reindexConcert(
            @PathVariable Long concertId
    ) {
        concertIndexService.indexConcert(concertId);
        return ResponseEntity.ok("공연 단건 재색인 요청 완료");
    }

    // 공연 전체 재색인
    @PostMapping("/reindex/all")
    public ResponseEntity<String> reindexAllConcerts() {
        int successCount =concertIndexService.reindexAllConcerts();
        return ResponseEntity.ok("전체 공연 재색인 완료: 성공" + successCount + "건");
    }

}