package com.dama.wanderwave.hashtag;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class HashTagService {

    private final HashTagRepository hashTagRepository;

    public Page<HashTag> getAllHashTags(Pageable pageable) {
        return hashTagRepository.findAll(pageable);
    }

    public Page<HashTag> getHashTagsByPrefix(String prefix, Pageable pageable) {
        return hashTagRepository.findByTitleStartingWith(prefix, pageable);
    }
}