package com.cinefinder.favorite.data.repository;

import com.cinefinder.favorite.data.Favorite;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    @Query(value = """
        SELECT movie_id FROM FAVORITE
        WHERE user_id = :userId
    """, nativeQuery = true)
    List<Long> findMovieIdListByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    void deleteByUserIdAndMovieId(Long userId, Long movieId);
}