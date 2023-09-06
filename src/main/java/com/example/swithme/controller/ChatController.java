package com.example.swithme.controller;

import com.example.swithme.dto.chat.*;
import com.example.swithme.entity.User;
import com.example.swithme.entity.chat.ChatMessage;
import com.example.swithme.repository.UserRepository;
import com.example.swithme.security.UserDetailsImpl;
import com.example.swithme.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;

    // 전체 채팅
    @MessageMapping("/allChat.sendMessage/1")
    @SendTo("/topic/public/1")
    public ChatMessageRequestDto sendAllMessage(@Payload ChatMessageRequestDto chatMessageRequestDto, java.security.Principal principal) {
        if( principal == null) {
            chatRoomService.save(new ChatMessage(chatMessageRequestDto.getSender(), chatMessageRequestDto.getContent()));
            return chatMessageRequestDto;
        }
        String name = principal.getName();
        User user = userRepository.findByUsername(name).get();

        chatRoomService.save(new ChatMessage(user.getNickname(), chatMessageRequestDto.getContent()));

        var sendMessage = ChatMessageRequestDto.builder()
                .sender(user.getNickname())
                .content(chatMessageRequestDto.getContent())
                .build();

        return sendMessage;
    }

    // 그룹 채팅
    @MessageMapping("/chat.sendMessage/{chatUrl}")
    @SendTo("/topic/public/{chatUrl}")
    public ChatMessageRequestDto sendMessage(@Payload ChatMessageRequestDto chatMessageRequestDto, java.security.Principal principal) {
        if( principal == null) {
            return chatMessageRequestDto;
        }
        String name = principal.getName();
        User user = userRepository.findByUsername(name).get();

        var sendMessage = ChatMessageRequestDto.builder()
                .sender(user.getNickname())
                .content(chatMessageRequestDto.getContent())
                .build();

        return sendMessage;
    }

    // 채팅내용 보내기
    @ResponseBody
    @GetMapping("/content")
    public List<ChatMessageResponseDto> sendRepositoryMessage() {
        List<ChatMessageResponseDto> message = chatRoomService.findMessage();
        return message;
    }


    // 채팅그룹 조회 페이지 이동
    @GetMapping
    public String chatPersonal(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        List<ChatRoomResponseDto> findAllChatRoom = chatRoomService.findAllById(user);
        model.addAttribute("chatRooms", findAllChatRoom);
        return "chat/chatgroup";
    }

    // 채팅그룹 초대 조회 페이지 이동
    @GetMapping("/alert")
    public String chatInvite(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        List<ChatRoomResponseDto> findAllChatRoom = chatRoomService.findAllInvite(user);
        model.addAttribute("chatRooms", findAllChatRoom);
        return "chat/chatinvite";
    }

    //채팅룸 생성
    @ResponseBody
    @PostMapping("/personal/room")
    public ResponseEntity creatChatRoom(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();
        chatRoomService.create(user);
        return new ResponseEntity("ok", HttpStatusCode.valueOf(200));
    }

    //수정 페이지로 팝업창
    @GetMapping("/room/edit")
    public String findChatRoom(@RequestParam(name = "id") Long id, Model model) {

        //채팅방 조회
        ChatRoomResponseDto chatRoom = chatRoomService.find(id);
        model.addAttribute("chatRoom", chatRoom);

        //채팅방 초대 또는 참가자 명단
        List<ChatUserResponseDto> inviteUser = chatRoomService.findUser(id);
        model.addAttribute("inviteUsers", inviteUser);

        return "chat/chatRoomEdit";
    }

    //채팅방 수정 : 제목, 타이틀
    @PostMapping("/room/edit")
    public String editChatRoom(@ModelAttribute ChatRoomRequestDto chatRoomRequestDto, @AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        chatRoomService.edit(chatRoomRequestDto, user);
        return "redirect:/chat/room/edit?id=" + chatRoomRequestDto.getId();
    }

    //채팅 초대 요청 보내기
    @PostMapping("/room/invite")
    public String inviteChatRoom(@ModelAttribute ChatRoomInviteRequestDTo chatRoomInviteRequestDTo, @AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        chatRoomService.invite(chatRoomInviteRequestDTo, user);

        ChatRoomResponseDto chatRoom = chatRoomService.find(chatRoomInviteRequestDTo.getId());
        model.addAttribute("chatRoom", chatRoom);

        //채팅방 초대 또는 참가자 명단
        List<ChatUserResponseDto> inviteUser = chatRoomService.findUser(chatRoomInviteRequestDTo.getId());
        model.addAttribute("inviteUsers", inviteUser);

        return "redirect:/chat/room/edit?id=" + chatRoom.getId();

    }

    //채팅 초대 요청 수락
    @PostMapping("/room/invite/accept")
    public String inviteAccept(@RequestParam Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();
        chatRoomService.inviteAccept(id, user);
        return "chat/personalinvite";
    }


    //채팅 초대 요청 거절
    @PostMapping("/room/invite/decline")
    public String inviteDecline(@RequestParam Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();
        chatRoomService.inviteDecline(id, user);
        return "chat/personalinvite";
    }

    // 채팅룸 멤버 삭제
    @DeleteMapping("/room/member")
    @ResponseBody
    public String deleteMember(@RequestBody ChatRoomRequestDto chatRoomRequestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();
        chatRoomService.delete(chatRoomRequestDto.getId(), user);
        return "ok";
    }

    // 채팅룸 삭제
    @DeleteMapping("/chatroom")
    @ResponseBody
    public String deleteChatRoom(@RequestBody ChatRoomRequestDto chatRoomRequestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userDetails.getUser();
        chatRoomService.deleteChatRoom(chatRoomRequestDto.getId(), user);
        return "ok";
    }
}
