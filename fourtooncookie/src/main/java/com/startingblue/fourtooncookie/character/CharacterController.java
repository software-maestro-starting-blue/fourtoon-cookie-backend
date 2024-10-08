package com.startingblue.fourtooncookie.character;

import com.startingblue.fourtooncookie.character.dto.request.CharacterSaveRequest;
import com.startingblue.fourtooncookie.character.dto.request.CharacterUpdateRequest;
import com.startingblue.fourtooncookie.character.dto.response.CharacterSavedResponse;
import com.startingblue.fourtooncookie.character.dto.response.CharacterSavedResponses;
import com.startingblue.fourtooncookie.character.service.CharacterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/character")
public final class CharacterController {

    private final CharacterService characterService;

    @PostMapping
    public ResponseEntity<HttpStatus> createCharacter(@Valid @RequestBody final CharacterSaveRequest request) {
        characterService.createCharacter(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @GetMapping
    public ResponseEntity<CharacterSavedResponses> readAllCharacters() {
        CharacterSavedResponses responses = CharacterSavedResponses.of(characterService.readAllCharacters());
        return ResponseEntity
                .ok(responses);
    }

    @GetMapping("/{characterId}")
    public ResponseEntity<CharacterSavedResponse> readCharacter(@PathVariable final Long characterId) {
        CharacterSavedResponse response = CharacterSavedResponse.of(characterService.readById(characterId));
        return ResponseEntity
                .ok(response);
    }

    @PutMapping("/{characterId}")
    public ResponseEntity<HttpStatus> updateCharacter(@PathVariable final Long characterId,
                                                        @Valid @RequestBody final CharacterUpdateRequest request) {
        characterService.updateCharacter(characterId, request);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @DeleteMapping("/{characterId}")
    public ResponseEntity<HttpStatus> deleteCharacter(@PathVariable final Long characterId) {
        characterService.deleteCharacter(characterId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
