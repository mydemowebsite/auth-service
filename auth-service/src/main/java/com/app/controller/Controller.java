package com.app.controller;


import com.app.Exceptions.UserAlreadyExistsException;
import com.app.model.*;
import com.app.config.JwtTokenUtil;
import com.app.service.JwtUserDetailsService;
import com.app.service.NoteService;
import com.app.svc.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
/*Spring RestController annotation is used to create RESTful web services using Spring MVC.
Spring RestController takes care of mapping request data to the defined request handler method.
Once response body is generated from the handler method, it converts it to JSON or XML response.*/

/* @CrossOrigin is used to allow cross-origin requests (to BACKEND in this case),
 which are disabled by default by spring-security*/

@RestController
@CrossOrigin("*")
public class Controller {


//   @Autowired
//    private KafkaTemplate<String, UserDTO> kafkaTemplate;

    private static final String TOPIC ="Kafka_Example_json12";

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private UserService userService;

    private ResponseEntity responseEntity;
    // "/authenticate" endpoint is exposed here
    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) throws Exception{
        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());

        final UserDetails userDetails = userDetailsService
                .loadUserByUsername(authenticationRequest.getUsername());

        final long userId = userService.getUserIdByName(authenticationRequest.getUsername());

        final String token = jwtTokenUtil.generateToken(userDetails, userId);

        return ResponseEntity.ok(new JwtResponse(token));
    }

    @RequestMapping(value = "/authenticateAdmin", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationTokenAdmin(@RequestBody JwtRequest authenticationRequest) throws Exception{
        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());

        final UserDetails userDetails = userDetailsService
                .loadUserByUsername(authenticationRequest.getUsername());

        final String token = jwtTokenUtil.generateTokenAdmin(userDetails);

        return ResponseEntity.ok(new JwtResponse(token));
    }

    // "/register" endpoint is exposed here
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ResponseEntity<?> saveUser(@RequestBody UserDTO user) throws Exception {
        try {
            UserEntity savedUser = userDetailsService.save(user);
            return ResponseEntity.ok(savedUser);
        }catch (UserAlreadyExistsException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @RequestMapping(value = "/activate", method = RequestMethod.PUT)
    public ResponseEntity<?> updateStatus(@RequestParam String numberAsString){
        return new ResponseEntity<>(userDetailsService.updateStatus(numberAsString), HttpStatus.OK);
    }

    @RequestMapping(value = "/addNote", method = RequestMethod.POST)
    public ResponseEntity<?> addNote(@RequestBody NoteDTO note) throws Exception {
        if(note.getContent().isEmpty()){
            return ResponseEntity.badRequest().build();
        }else {
            return ResponseEntity.ok(noteService.createNote(note));
        }
    }

    // Fetches all notes created by user
    @RequestMapping(value = "/getNotes", method = RequestMethod.GET)
    public ResponseEntity<?> getNotes() throws Exception {

        return ResponseEntity.ok(noteService.getNotes());
    }

    @RequestMapping(value = "/updateNote/{noteId}", method = RequestMethod.PUT)
    public ResponseEntity<?> updateNote(@RequestBody NoteDTO note, @PathVariable long noteId) throws Exception {
        Note updateNote = noteService.updateNote(noteId, note);
        if(updateNote.getCreatedOn() != null){
            return ResponseEntity.ok(noteService.updateNote(noteId, note));
        }else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/deleteNote/{noteId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteNote(@PathVariable long noteId) throws Exception {
        boolean isNoteDeleted =  noteService.deleteNote(noteId);
        if(isNoteDeleted){
            return ResponseEntity.ok().build();
        }else {
            return ResponseEntity.notFound().build();
        }

    }

    private void authenticate(String username, String password) throws Exception {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
//         kafkaTemplate.send(TOPIC, new UserDTO(username,"",""));
        } catch (DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }
}
