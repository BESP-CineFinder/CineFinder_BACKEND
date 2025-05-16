package com.cinefinder.favorite.service;

import com.cinefinder.favorite.data.dto.FavoriteRequestDto;
import com.cinefinder.favorite.data.model.FavoriteMovie;
import com.cinefinder.favorite.data.repository.FavoriteRepository;
import com.cinefinder.favorite.mapper.FavoriteMapper;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.Movie;
import com.cinefinder.movie.service.MovieDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final MovieDetailService movieDetailService;

    @Transactional
    public void updateFavoriteInfo(FavoriteRequestDto favoriteRequestDto) {
        try {
            // 1. 좋아요 여부 확인
            boolean isExistFavorite = favoriteRepository.existsByUserIdAndMovieId(
                    favoriteRequestDto.getUserId(),
                    favoriteRequestDto.getMovieId()
            );
            log.info("✅ 좋아요 여부 {}", isExistFavorite);

            // 2. 좋아요 여부에 따라 토글 처리
            if (isExistFavorite) {
                favoriteRepository.deleteByUserIdAndMovieId(
                        favoriteRequestDto.getUserId(),
                        favoriteRequestDto.getMovieId()
                );
                log.info("👎 좋아요 존재 ... 좋아요 취소");
            } else {
                favoriteRepository.save(FavoriteMapper.toEntity(favoriteRequestDto));
                log.info("👍 좋아요 없음 ... 좋아요 등록");
            }
        } catch (Exception e) {
            throw new CustomException(ApiStatus._OPERATION_FAIL, "좋아요 데이터 갱신 중 오류 발생");
        }
    }

    public Boolean checkFavorite(Long userId, Long movieId) {
        try {
            return favoriteRepository.existsByUserIdAndMovieId(userId, movieId);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "좋아요 여부 조회 중 오류 발생");
        }
    }

    public List<FavoriteMovie> getFavoriteMovieListByUser(Long userId) {
        try {
            // 1. 사용자가 좋아요 등록한 영화 ID 목록 조회
            List<Long> movieIdList = favoriteRepository.findMovieIdListByUserId(userId);
            log.info("✅ ID {} 사용자의 좋아요 등록 영화 개수 : {}", userId, movieIdList.size());

            // 2. 영화 ID 목록이 없을 경우
            if (movieIdList.isEmpty()) {
                log.info("‼️ 영화 ID 목록 없음");
                return null;
            }

            // 3.  영화 ID 목록에 해당하는 정보 조회
            List<Movie> movieList = movieDetailService.getFavoriteMovieList(movieIdList);
            log.info("✅ ID 기반으로 조회한 영화정보 목록 개수 : {}", movieIdList.size());

            // 4. 영화 ID 목록 개수와 영화정보 목록 개수가 일치하지 않을 경우
            if (movieIdList.size() != movieList.size()) {
                throw new RuntimeException("ID에 해당하는 영화정보를 모두 찾을 수 없음");
            }

            // 5. 반환할 영화목록 생성
            List<FavoriteMovie> favoriteMovieList = new ArrayList<>();
            for (Movie movie : movieList) favoriteMovieList.add(FavoriteMapper.toFavoriteMovie(movie));

            return favoriteMovieList;
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "좋아요 영화목록 조회 중 오류 발생");
        }
    }
}
