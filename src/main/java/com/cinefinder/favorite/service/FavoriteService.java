package com.cinefinder.favorite.service;

import com.cinefinder.favorite.data.dto.FavoriteRequestDto;
import com.cinefinder.favorite.data.dto.FavoriteResponseDto;
import com.cinefinder.favorite.data.repository.FavoriteRepository;
import com.cinefinder.favorite.mapper.FavoriteMapper;
import com.cinefinder.favorite.model.FavoriteMovie;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.Movie;
import com.cinefinder.movie.service.MovieService;
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
    private final MovieService movieService;

    @Transactional
    public void updateFavoriteInfo(FavoriteRequestDto favoriteRequestDto) {
        try {
            boolean isExistFavorite = favoriteRepository.existsByUserIdAndMovieId(
                    favoriteRequestDto.getUserId(),
                    favoriteRequestDto.getMovieId()
            );
            log.info("✅ 좋아요 여부 {}", isExistFavorite);

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

    public List<FavoriteResponseDto> checkFavorite(Long userId, List<Long> movieIdList) {
        try {
            List<FavoriteResponseDto> favoriteResponseDtoList = new ArrayList<>();
            for (Long movieId : movieIdList) {
                favoriteResponseDtoList.add(FavoriteResponseDto.builder()
                    .userId(userId)
                    .movieId(movieId)
                    .isFavorite(favoriteRepository.existsByUserIdAndMovieId(userId, movieId))
                    .build());
            }

            return favoriteResponseDtoList;
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "좋아요 여부 조회 중 오류 발생");
        }
    }

    public List<FavoriteMovie> getFavoriteMovieListByUser(Long userId) {
        try {
            List<Long> movieIdList = favoriteRepository.findMovieIdListByUserId(userId);
            log.info("✅ ID {} 사용자의 좋아요 등록 영화 개수 : {}", userId, movieIdList.size());

            if (movieIdList.isEmpty()) {
                log.info("‼️ 영화 ID 목록 없음");
                return null;
            }

            List<Movie> movieList = movieService.getFavoriteMovieList(movieIdList);
            log.info("✅ ID 기반으로 조회한 영화정보 목록 개수 : {}", movieIdList.size());

            if (movieIdList.size() != movieList.size()) {
                throw new RuntimeException("ID에 해당하는 영화정보를 모두 찾을 수 없음");
            }

            List<FavoriteMovie> favoriteMovieList = new ArrayList<>();
            for (Movie movie : movieList) favoriteMovieList.add(FavoriteMapper.toFavoriteMovie(movie));

            return favoriteMovieList;
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "좋아요 영화목록 조회 중 오류 발생");
        }
    }
}