package io.github.lvoxx.media_service.cloudinary;

import io.github.lvoxx.media_service.utils.MediaType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class FolderType {
    public static String getFolderFromMediaType(MediaType mediaType) {
        return switch (mediaType) {
            case IMAGE -> "images";
            case VIDEO -> "videos";
            case ATTACHMENT -> "attachments";
            case CHAT_MEDIA -> "chat";
            case AVATAR -> "avatars";
            case COVER -> "covers";
        };
    }
}
