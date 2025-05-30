package com.cinefinder.favorite.service;

import com.cinefinder.favorite.data.dto.FavoriteRequestDto;
import com.cinefinder.favorite.data.dto.FavoriteResponseDto;
import com.cinefinder.favorite.data.repository.FavoriteRepository;
import com.cinefinder.favorite.mapper.FavoriteMapper;
import com.cinefinder.favorite.model.FavoriteMovie;
import com.cinefinder.global.exception.custom.CustomException;
import com.cinefinder.global.util.statuscode.ApiStatus;
import com.cinefinder.movie.data.entity.Movie;
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

            if (isExistFavorite) {
                favoriteRepository.deleteByUserIdAndMovieId(
                        favoriteRequestDto.getUserId(),
                        favoriteRequestDto.getMovieId()
                );
            } else {
                favoriteRepository.save(FavoriteMapper.toEntity(favoriteRequestDto));
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

            if (movieIdList.isEmpty()) {
                log.info("‼️ 영화 ID 목록 없음");
                return null;
            }

            List<Movie> movieList = movieService.getFavoriteMovieList(movieIdList);

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

    public Long countFavoriteMovieList(Long movieId) {
        try {
            return favoriteRepository.countByMovieId(movieId);
        } catch (Exception e) {
            throw new CustomException(ApiStatus._READ_FAIL, "ID에 해당하는 영화의 좋아요 개수 조회 중 오류 발생");
        }
    }
}