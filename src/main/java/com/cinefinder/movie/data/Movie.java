package com.cinefinder.movie.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "MOVIE")
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String cgvCode;

    @Column
    private String megaBoxCode;

    @Column
    private String lotteCinemaCode;

    @Column(unique = true)
    private String title;

    @Column
    private String titleEng;

    @Column
    private String nation;

    @Column
    private String genre;

    @Column
    private String ratingGrade;

    @Column
    private String releaseDate;

    @Column
    private String runtime;

    @Column
    private String directors;

    @Column
    private String actors;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String posters;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String stlls;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String plotText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String vods;

    public void updateMovie(Movie movie) {
        this.cgvCode = movie.getCgvCode();
        this.megaBoxCode = movie.getMegaBoxCode();
        this.lotteCinemaCode = movie.getLotteCinemaCode();
        this.title = movie.getTitle();
        this.titleEng = movie.getTitleEng();
        this.nation = movie.getNation();
        this.genre = movie.getGenre();
        this.ratingGrade = movie.getRatingGrade();
        this.releaseDate = movie.getReleaseDate();
        this.runtime = movie.getRuntime();
        this.directors = movie.getDirectors();
        this.actors = movie.getActors();
        this.posters = movie.getPosters();
        this.stlls = movie.getStlls();
        this.plotText = movie.getPlotText();
        this.vods = movie.getVods();
    }
}
