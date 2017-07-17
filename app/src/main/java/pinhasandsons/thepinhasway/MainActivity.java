package pinhasandsons.thepinhasway;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {
    static final String PS_VERSION = "Passover17";
    static final String LOG_TAG_NAME = "thepinhasway";
    static String ROOT_STORAGE = Environment.getExternalStorageDirectory().toString();
    static final String PINHAS_DIR_PATH = ROOT_STORAGE + "/Pinhas_and_Sons";
    static final String PINHAS_VERSION_DIR_PATH = PINHAS_DIR_PATH + "/" + PS_VERSION;
    static final String HEB_BACKUP_PATH = PINHAS_VERSION_DIR_PATH + "/heb_backup";
    static final String WAZE_HEB_DIR_PATH = ROOT_STORAGE + "/waze/sound/heb";
    static final String ASSETS_FILES_EXTENTION = "mp3";
    static final String PREFS_NAME = "PinhasAndSonsPrefsFile";
    static final MediaPlayer MEDIA_PLAYER = new MediaPlayer();
    static final String PINHAS_COMPOSER_TAG = "Pinhas";

    static final String TMP_PROP_DIR = PINHAS_VERSION_DIR_PATH + "/tmp";
    static MediaMetadataRetriever mmr = new MediaMetadataRetriever();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File pinhasDir = new File(PINHAS_DIR_PATH);
        if (!pinhasDir.exists()) {
            try {
                pinhasDir.mkdir();
            } catch (SecurityException se) {
                Log.e(LOG_TAG_NAME, se.toString());
                return;
            }
        }
        File pinhasVersionDir = new File(PINHAS_VERSION_DIR_PATH);
        if (!pinhasVersionDir.exists()) {
            try {
                pinhasVersionDir.mkdir();
            } catch (SecurityException se) {
                Log.e(LOG_TAG_NAME, se.toString());
                return;
            }
        }

        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.getBoolean("my_first_time", true)) {

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
            alertDialog.setTitle("תנאי שימוש");
            alertDialog.setMessage("על מנת להשתמש באפליקציה עליך לאשר את הסעיפים הבאים:\n" +
                    "הפעלת האפליקציה, השימוש בה וההסתמכות על ההנחיות הקוליות בה הינם באחריות המשתמש בלבד. אין המפתח אחראי לכל נזק, פגיעה או אובדן שנגרמו או שנטען שנגרמו, במישרין או בעקיפין, כלפי המשתמש או כלפי כל צד שלישי, כתוצאה מהתקנת או שימוש באפליקציה.\n" +
                    "זכויות היוצרים על ההנחיות הקוליות המותקנות שייכות למפתח ואין לעשות בהן כל שימוש מסחרי לרבות העתקה, הקלטה, פרסום או הפצה ללא אישור ישיר של המפתח.\n" +
                    "\n" +
                    "תודה על שיתוף הפעולה ונסיעה מהנה!\n" +
                    "פנחס ובניו");
            alertDialog.setPositiveButton("אני מאשר/ת",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            settings.edit().putBoolean("my_first_time", false).commit();
                            new BackgroundSound().execute();
                            dialog.dismiss();
                        }
                    });
            alertDialog.setNegativeButton("אני לא מאשר/ת",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.finish();
                            System.exit(0);
                        }
                    });
            alertDialog.show();
        }
        else
        {
            new BackgroundSound().execute();
        }
    }

    public void switchToPinhasSounds(View view) {
        Log.i(LOG_TAG_NAME, "switchToPinhasSounds");
        File pinhasHebBackupDir = new File(HEB_BACKUP_PATH);
        // if the directory does not exist, create it
        if (!pinhasHebBackupDir.exists()) {
            try {
                Log.i(LOG_TAG_NAME, "creating "+HEB_BACKUP_PATH+" folder");
                pinhasHebBackupDir.mkdir();
            } catch (SecurityException se) {
                Log.e(LOG_TAG_NAME, se.toString());
                return;
            }
        }
        if (!backupAndCopyFromAssets(WAZE_HEB_DIR_PATH, HEB_BACKUP_PATH)) {
            if (m_revertFromBackup(pinhasHebBackupDir, WAZE_HEB_DIR_PATH)){
                Toast.makeText(getApplicationContext(), "התקנת הנחיות קוליות פנחס ובניו נכשלה. הנחיות מקוריות הוחזרו", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "התקנת הנחיות קוליות פנחס ובניו נכשלה", Toast.LENGTH_LONG).show();
            }
        }
        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            alertMessageWithDismissButton("הנחיות קוליות פנחס ובניו הותקנו בהצלחה!\n" +
                    "נא ודא ששפת השמע המוגדרת בwaze היא \"עברית\".", "ובניו!");

        }
    }

    private void alertMessageWithDismissButton(String alertMessage, String dismissButton){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(alertMessage)
                .setCancelable(false)
                .setNeutralButton(dismissButton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }


    private boolean backupAndCopyFromAssets(String destDir, String backupDir) {
        Toast.makeText(getApplicationContext(), "(סבלנות, הפעולה לוקחת מספר שניות)", Toast.LENGTH_LONG).show();
        AssetManager assetManager = getAssets();
        if (assetManager == null){
            Log.e(LOG_TAG_NAME, "no assets");
            return false;
        }
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e(LOG_TAG_NAME, e.toString());
            return false;
        }
        if (files == null || files.length == 0){
            return false;
        }

        Log.i(LOG_TAG_NAME, "before copy");
        for (String filename : files) {
            //skip file if file is not an mp3 file
            if (!filename.contains("."+ASSETS_FILES_EXTENTION)) {
                continue;
            }
            InputStream in = null;
            OutputStream out = null;
            try {
                AssetFileDescriptor assetFD = assetManager.openFd(filename);
                if (assetFD == null){
                    return false;
                }
                long assetFileLen = assetFD.getLength();//current file's size
                in = assetManager.open(filename);
                File backupFile = new File(backupDir, filename);
                File destFile = new File(destDir, filename);

                //backup and copy only if waze file exists and is not our current version file
                if (destFile.exists() && !isFileCurrentVersion(destFile)) {
                    if (copyFileUsingFileStreams(destFile, backupFile) && backupFile.length() == destFile.length()) {//backup file
                        out = new FileOutputStream(destFile);
                        //copy to destination
                        if (!copyFile(in, out)){
                            //copy failed
                            return false;
                        }
                        //copy succeeded
                    } else {
                        Log.w(LOG_TAG_NAME, "file "+filename+" was not copied.");
                        continue;
                    }
                }
            } catch (IOException e) {
                Log.e(LOG_TAG_NAME, e.toString());
                return false;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        return false;
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        return false;
                    }
                }
            }
        }
        Log.i(LOG_TAG_NAME, "the pinhas way sound package was successfully installed");
        return true;
    }

    public void revertToBackupSounds(View view) {
        Log.i(LOG_TAG_NAME, "revertToBackupSounds");
        TextView textView = (TextView) findViewById(R.id.explanation);
        textView.setVisibility(View.INVISIBLE);
        File pinhasHebBackupDir = new File(HEB_BACKUP_PATH);
        if (!pinhasHebBackupDir.exists()){
            Toast.makeText(getApplicationContext(), "ההנחיות הקוליות של פנחס ובניו לא הותקנו", Toast.LENGTH_LONG).show();
            return;
        }
        File[] hebBackupFiles = pinhasHebBackupDir.listFiles();
        if (hebBackupFiles == null || hebBackupFiles.length == 0) {
            Toast.makeText(getApplicationContext(), "ההנחיות הקוליות של פנחס ובניו לא הותקנו", Toast.LENGTH_LONG).show();
            return;
        }

        if (!m_revertFromBackup(pinhasHebBackupDir, WAZE_HEB_DIR_PATH)) {
            Toast.makeText(getApplicationContext(), "החזרת הנחיות קוליות מקוריות נכשלה", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getApplicationContext(), "הנחיות וייז המקוריות הוחזרו", Toast.LENGTH_LONG).show();
    }

    //backup exists
    private boolean m_revertFromBackup(File BackupDir, String dest_dir){
        if (BackupDir == null)
            return false;

        if (!BackupDir.exists())
            return true;

        File[] hebBackupFiles = BackupDir.listFiles();
        if (hebBackupFiles == null){
            return false;
        }

        boolean copySucceeded = true;


        Log.i(LOG_TAG_NAME, "start revert sounds");
        for (File backupFile : hebBackupFiles) {
            String fileName = backupFile.getName();
            File destinationFile = new File(dest_dir + "/" + fileName);
            //revert file only if the current waze file is our file (was copied previously), or it does'nt exist
            if (!destinationFile.exists() || isPinhasFile(destinationFile)) {
                if (!copyFileUsingFileStreams(backupFile, destinationFile)) {
                    copySucceeded = false;
                    continue;
                }
            }
        }

        return copySucceeded;
    }

    private Boolean createTmpDir(){
        File tmp_dir = new File(TMP_PROP_DIR);
        if (!tmp_dir.exists()) {
            try {
                Log.i(LOG_TAG_NAME, "creating "+TMP_PROP_DIR+" folder");
                tmp_dir.mkdir();
            } catch (SecurityException se) {
                Log.e(LOG_TAG_NAME, se.toString());
                return false;
            }
        }
        return true;
    }

    /**
     * checks whether the given file is of the current pinhas version
     * @return
     */
    private Boolean isFileCurrentVersion(File f){
        if (!createTmpDir())
            return false;

        String fileName = f.getName();
        String tmp_file = TMP_PROP_DIR + "/" + fileName;
        File tmpPropFile = new File(tmp_file);
        if (!copyFileUsingFileStreams(f, tmpPropFile)) {
            return false;
        }
        try{
            mmr.setDataSource(tmp_file);
            String composer = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER);
            if (composer != PINHAS_COMPOSER_TAG){
                return false;
            }
            String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (album != PS_VERSION){
                return false;
            }
        }
        catch (Exception e){
            Log.e(LOG_TAG_NAME, e.toString());
        }

        tmpPropFile.delete();
        return true;
    }

    /**
     * checks whether the given file is a pinhas file
     * @return
     */
    private Boolean isPinhasFile(File f){
        if (!createTmpDir())
            return false;

        String fileName = f.getName();
        String tmp_file = TMP_PROP_DIR + "/" + fileName;
        File tmpPropFile = new File(tmp_file);
        if (!copyFileUsingFileStreams(f, tmpPropFile)) {
            return false;
        }
        try{
            mmr.setDataSource(tmp_file);
            String composer = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER);
            if (composer != PINHAS_COMPOSER_TAG){
                return false;
            }
        }
        catch (Exception e){
            Log.e(LOG_TAG_NAME, e.toString());
        }
        tmpPropFile.delete();
        return true;
    }

    private boolean copyFile(InputStream in, OutputStream out) {
        byte[] buffer = new byte[1024];
        int read;
        try {
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG_NAME, e.toString());
            return false;
        }
        return true;
    }

    private boolean copyFileUsingFileStreams(File source, File dest) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(dest);
            copyFile(input, output);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(LOG_TAG_NAME, e.toString());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG_NAME, e.toString());
            return false;
        } finally {
            try {
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG_NAME, e.toString());
                return false;
            }
        }
        return true;
    }


    public void openBrowser(View view){

        //Get url from tag
        String url = (String)view.getTag();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        //pass the url to intent data
        intent.setData(Uri.parse(url));

        startActivity(intent);
    }

    public class BackgroundSound extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                AssetFileDescriptor descriptor = getAssets().openFd("Arrive.mp3");
                MEDIA_PLAYER.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                descriptor.close();

                MEDIA_PLAYER.prepare();
                MEDIA_PLAYER.setVolume(1f, 1f);
                MEDIA_PLAYER.setLooping(false);
                MEDIA_PLAYER.start();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

    }



}