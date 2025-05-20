package com.cinefinder.movie.data.repository;

import com.cinefinder.movie.data.Movie;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByMovieKey(String title);
    Movie findByCgvCode(String cgvCode);
    Movie findByLotteCinemaCode(String lotteCode);
    Movie findByMegaBoxCode(String megaCode);

    @Query(value = """
        SELECT *
        FROM MOVIE
        WHERE id IN :movieIdList
    """, nativeQuery = true)
    List<Movie> findByMovieIdList(@Param("movieIdList") List<Long> movieIdList);

    @Query(value = """
        SELECT id
        FROM movie
        WHERE movie_key = :movieKey
    """, nativeQuery = true)
    Long findMovieIdByMovieKey(@Param("movieKey") String movieKey);
}
