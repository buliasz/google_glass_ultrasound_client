package com.ooliash.android.glass.usg_client;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.google.android.glass.media.Sounds;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UsgSessionMenuHandler {

    private static final String LOG_TAG = "USG";
    private final UsgSessionActivity context;

    UsgSessionMenuHandler(UsgSessionActivity context) {
        this.context = context;
    }

    public void HandleItem(int itemId) {
        context.playSoundEffect(Sounds.TAP);
        switch (itemId) {
            case R.id.freeze_option:
                context.sendCommand(Command.FREEZE);
                break;
            case R.id.gain_up:
                context.sendCommand(Command.GAIN_UP);
                break;
            case R.id.gain_down:
                context.sendCommand(Command.GAIN_DOWN);
                break;
            case R.id.area_up:
                context.sendCommand(Command.AREA_UP);
                break;
            case R.id.area_down:
                context.sendCommand(Command.AREA_DOWN);
                break;
            case R.id.save_picture_option:
                savePicture();
                break;
            case R.id.sine1_25:
                context.sendCommand(Command.SIGNAL_SINE_1_25);
                break;
            case R.id.sine4_25:
                context.sendCommand(Command.SIGNAL_SINE_4_25);
                break;
            case R.id.sine6_25:
                context.sendCommand(Command.SIGNAL_SINE_6_25);
                break;
            case R.id.sine16_25:
                context.sendCommand(Command.SIGNAL_SINE_16_25);
                break;
            case R.id.bit13_20:
                context.sendCommand(Command.SIGNAL_13_BIT_20);
                break;
            case R.id.bit13_35:
                context.sendCommand(Command.SIGNAL_13_BIT_35);
                break;
            case R.id.bit16_chirp:
                context.sendCommand(Command.SIGNAL_16_BIT_CHIRP);
                break;
            case R.id.linear:
                context.sendCommand(Command.PALETTE_LINEAR);
                break;
            case R.id.log1_5:
                context.sendCommand(Command.PALETTE_LOG_1_5);
                break;
            case R.id.log1_75:
                context.sendCommand(Command.PALETTE_LOG_1_75);
                break;
            case R.id.log2_0:
                context.sendCommand(Command.PALETTE_LOG_2_0);
                break;
            case R.id.log3_0:
                context.sendCommand(Command.PALETTE_LOG_3_0);
                break;
            case R.id.close_session:
                context.finish();
                break;
            case R.id.cancel:
                break;
            default:
                Log.e(LOG_TAG, "Unknown session menu item ID."); // + item.getTitle()
        }
    }

    private void savePicture() {
        try {
            File outputFile = preparePictureDirectoryAndFile();
            outputFile.createNewFile();
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(outputFile));
            bos.write(context.getLastUsgPictureBytes());
            bos.flush();
            bos.close();
            context.audioManager.playSoundEffect(Sounds.SUCCESS);
            // viewPicture(outputFile); - not working on GG Exp
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            context.errorMessage("Couldn't create picture file.");
        } catch (IOException e) {
            e.printStackTrace();
            context.errorMessage("Couldn't write picture to file.");
        }
    }

    /**
     * Opens image passed as File argument in a system image viewer.
     * Unfortunately it seems to be no default image viewer on Google Glass Exp
     * @param pictureFile File with an image.
     */
    private void viewPicture(File pictureFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri picUri = Uri.fromFile(pictureFile);
        Log.d(LOG_TAG, "Opening viewer for: " + picUri.toString());
        intent.setDataAndType(picUri, "image/*");
        context.startActivity(intent);
    }

    /**
     * Prepare USG picture directory and file for new picture.
     * @return Output file.
     */
    private File preparePictureDirectoryAndFile() {
        // Create main pictures folder if it doesn't exist
        File picturesPublicDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        picturesPublicDirectory.mkdirs();
        // File parent = context.getFilesDir();
        File sessionsRootDirectory  = new File(picturesPublicDirectory, "usg_pictures");
        sessionsRootDirectory.mkdirs();

        // Create session folder
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH_mm");
        File sessionDirectory =
                new File(sessionsRootDirectory, dateFormat.format(context.sessionStartTime));
        sessionDirectory.mkdirs();

        // Create image file
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss");
        return new File(sessionDirectory, dateFormat.format(new Date()) + ".jpg");
    }
}
