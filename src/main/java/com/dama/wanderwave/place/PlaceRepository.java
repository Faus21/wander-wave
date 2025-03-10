package com.dama.wanderwave.place;

import com.dama.wanderwave.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, String> {

    List<Place> findAllByPost(Post post);

}
