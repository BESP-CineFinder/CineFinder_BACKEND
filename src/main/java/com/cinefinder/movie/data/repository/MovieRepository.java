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
    Optional<Movie> findByMovieKey(String movieKey);
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
        SELECT m.id
        FROM Movie m
        WHERE m.movieKey = :movieKey
    """)
    Long findMovieIdByMovieKey(@Param("movieKey") String movieKey);

    @Query("SELECT m.id FROM Movie m")
    List<Long> findAllMovieIds();

    @Query("""
        SELECT m FROM Movie m
        WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(m.directors) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(m.actors) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY
          CASE
            WHEN LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 0
            ELSE 1
          END
    """)
    List<Movie> searchMoviesByKeyword(@Param("keyword") String keyword);
}
