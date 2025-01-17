import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.imageio.ImageIO;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DiscordBot extends ListenerAdapter {

    public static void main(String[] args) throws LoginException {
        JDABuilder builder = JDABuilder.createDefault(System.getenv("DISCORD_TOKEN"));
        builder.addEventListeners(new DiscordBot());
        builder.build();
    }

    public static final String THIS_BOT_ID = "822494927542943814";
    private static final long MAX_FILE_SIZE = 8 * 1024 * 1024; // 8MB

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        System.out.println(
                event.getGuild().getName() + ": " +
                        event.getAuthor().getId() + ": " + event.getMessage().getContentRaw());
        if (!event.getMessage().getAttachments().isEmpty() && !event.getAuthor().getId().equals(THIS_BOT_ID)) {
            event.getMessage().getAttachments().forEach(attachment -> {
                if (attachment.getContentType().startsWith("image/")) {
                    try {
                        // Download the image
                        File tempImage = new File("temp_" + attachment.getFileName());
                        attachment.downloadToFile(tempImage).thenAccept(downloadedFile -> {
                            try {
                                File upscaledImage = new File("upscaled_" + attachment.getFileName());
                                int model_scale = 4;
                                int output_scale = 4;

                                while (model_scale >= 2) {
                                    System.out.println("Attempting with model_scale: " + model_scale + " and output_scale: " + output_scale);
                                    // Run the upscayl CLI
                                    ProcessBuilder processBuilder = new ProcessBuilder(
                                            "C:\\Users\\jared\\upscayl-bin-20240601-103425-windows\\upscayl-bin.exe",
                                            "-i", downloadedFile.getAbsolutePath(),
                                            "-o", upscaledImage.getAbsolutePath(),
                                            "-m", "C:\\Program Files\\Upscayl\\resources\\models",
                                            "-n", "digital-art-4x",
                                            "-z", String.valueOf(model_scale),
                                            "-s", String.valueOf(output_scale)
                                    );
                                    processBuilder.inheritIO().start().waitFor();

                                    // Check the file size
                                    System.out.println(upscaledImage.getAbsolutePath() + " size: " + Files.size(upscaledImage.toPath()) + "/ max size: " + MAX_FILE_SIZE + " bytes");
                                    if (upscaledImage.length() < MAX_FILE_SIZE) {
                                        // Send the upscaled image back to the channel with the original message
                                        event.getChannel().sendMessage(event.getMessage().getContentRaw()).addFile(upscaledImage).queue(message -> {
                                                    // Clean up temporary files
                                                    tempImage.delete();
                                                    upscaledImage.delete();
                                                    // Delete the original message
                                                    if (event.getAuthor().isBot()) {
                                                        event.getMessage().delete().queue();
                                                    }
                                                },
                                                throwable -> {
                                                    // Handle the error
                                                    System.err.println("Failed to send file: " + throwable.getMessage());
                                                    tempImage.delete();
                                                    upscaledImage.delete();
                                                });
                                        break; // Exit the loop if successful
                                    } else {
                                        // Adjust parameters
                                        if (output_scale == 1) {
                                            output_scale = model_scale--;
                                        } else {
                                            output_scale--;
                                        }
                                    }
                                }

                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
