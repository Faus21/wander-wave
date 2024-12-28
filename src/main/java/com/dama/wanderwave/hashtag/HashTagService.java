package com.dama.wanderwave.hashtag;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class HashTagService {

    private final HashTagRepository hashTagRepository;

    public List<String> getAllHashTags() {
        return hashTagRepository.findAll()
                .stream()
                .map(HashTag::getTitle)
                .toList();
    }

    public List<String> getHashTagsByPrefix(String prefix) {
        return hashTagRepository.findByTitleStartingWith(prefix).stream()
                .map(HashTag::getTitle)
                .toList();
    }
}