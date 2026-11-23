package com.likelion.session.service;

import com.likelion.session.domain.Board;
import com.likelion.session.domain.DeleteLog;
import com.likelion.session.dto.BoardCreateRequest;
import com.likelion.session.dto.BoardPageResponse;
import com.likelion.session.dto.BoardResponse;
import com.likelion.session.dto.BoardUpdateRequest;
import com.likelion.session.dto.DeleteLogResponse;
import com.likelion.session.repository.BoardRepository;
import com.likelion.session.repository.DeleteLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final DeleteLogRepository deleteLogRepository;

    /*
        게시글 생성
        - 요청 DTO를 Entity로 바꿔 저장하고, 저장된 결과를 응답 DTO로 반환
     */
    public BoardResponse create(BoardCreateRequest request) {
        Board board = new Board(
                request.getTitle(),
                request.getContent(),
                request.getWriter()
        );

        Board savedBoard = boardRepository.save(board);

        return BoardResponse.from(savedBoard);
    }

    /*
        게시글 전체 조회
     */
    @Transactional(readOnly = true)
    public List<BoardResponse> findAll() {
        return boardRepository.findAll()
                .stream()
                .map(BoardResponse::from)
                .toList();
    }

    /*
        게시글 단건 조회
        - 없으면 예외 발생
     */
    @Transactional(readOnly = true)
    public BoardResponse findById(Long id) {
        Board board = findBoardById(id);

        return BoardResponse.from(board);
    }

    /*
        게시글 수정
        - 엔티티의 update 메서드로 값 변경 → JPA 변경 감지로 반영
     */
    public BoardResponse update(Long id, BoardUpdateRequest request) {
        Board board = findBoardById(id);

        board.update(request.getTitle(), request.getContent());

        return BoardResponse.from(board);
    }

    /*
        게시글 삭제
        - 삭제 이력을 먼저 저장한 뒤 게시글 삭제
     */
    public void delete(Long id) {
        Board board = findBoardById(id);

        DeleteLog deleteLog = new DeleteLog(board.getId(), board.getTitle(), board.getWriter());
        deleteLogRepository.save(deleteLog);

        boardRepository.delete(board);
    }

    /*
        제목으로 검색
     */
    @Transactional(readOnly = true)
    public List<BoardResponse> searchByTitle(String keyword) {
        return boardRepository.findByTitleContaining(keyword)
                .stream()
                .map(BoardResponse::from)
                .toList();
    }

    /*
        작성자로 검색
     */
    @Transactional(readOnly = true)
    public List<BoardResponse> searchByWriter(String writer) {
        return boardRepository.findByWriter(writer)
                .stream()
                .map(BoardResponse::from)
                .toList();
    }

    /*
        통합 검색 (제목 + 내용)
     */
    @Transactional(readOnly = true)
    public List<BoardResponse> search(String keyword) {
        return boardRepository.searchByKeyword(keyword)
                .stream()
                .map(BoardResponse::from)
                .toList();
    }

    /*
        페이지네이션 적용 전체 조회
        - page: 몇 번째 페이지 (0부터 시작)
        - size: 페이지당 몇 개
     */
    @Transactional(readOnly = true)
    public BoardPageResponse findAllWithPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Board> boardPage = boardRepository.findAll(pageable);

        List<BoardResponse> content = boardPage.getContent()
                .stream()
                .map(BoardResponse::from)
                .toList();

        return BoardPageResponse.builder()
                .content(content)
                .totalPages(boardPage.getTotalPages())
                .totalElements(boardPage.getTotalElements())
                .currentPage(boardPage.getNumber())
                .size(boardPage.getSize())
                .build();
    }

    /*
        삭제 이력 전체 조회
     */
    @Transactional(readOnly = true)
    public List<DeleteLogResponse> findAllDeleteLogs() {
        return deleteLogRepository.findAll()
                .stream()
                .map(deleteLog -> DeleteLogResponse.builder()
                        .id(deleteLog.getId())
                        .boardId(deleteLog.getBoardId())
                        .boardTitle(deleteLog.getBoardTitle())
                        .writer(deleteLog.getWriter())
                        .deletedAt(deleteLog.getDeletedAt())
                        .build())
                .toList();
    }

    private Board findBoardById(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + boardId));
    }
}