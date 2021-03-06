package org.firstinspires.ftc.teamcode.teamcode2020;

import android.graphics.Bitmap;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.ThreadPool;
import com.vuforia.Frame;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.function.Consumer;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackableDefaultListener;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This 2016-2017 OpMode illustrates the basics of using the Vuforia localizer to determine
 * positioning and orientation of robot on the FTC field.
 * The code is structured as a LinearOpMode
 *
 * Vuforia uses the phone's camera to inspect it's surroundings, and attempt to locate target images.
 *
 * When images are located, Vuforia is able to determine the position and orientation of the
 * image relative to the camera.  This sample code than combines that information with a
 * knowledge of where the target images are on the field, to determine the location of the camera.
 *
 * This example assumes a "diamond" field configuration where the red and blue alliance stations
 * are adjacent on the corner of the field furthest from the audience.
 * From the Audience perspective, the Red driver station is on the right.
 * The two vision target are located on the two walls closest to the audience, facing in.
 * The Stones are on the RED side of the field, and the Chips are on the Blue side.
 *
 * A final calculation then uses the location of the camera on the robot to determine the
 * robot's location and orientation on the field.
 *
 * @see VuforiaLocalizer
 * @see VuforiaTrackableDefaultListener
 * see  ftc_app/doc/tutorial/FTC_FieldCoordinateSystemDefinition.pdf
 *
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list.
 *
 * IMPORTANT: In order to use this OpMode, you need to obtain your own Vuforia license key as
 * is explained below.
 */

@TeleOp(name="Concept: Vuforia Nav Webcam", group ="Concept")
@Disabled
public class VuforiaRecognition extends LinearOpMode {

    public static final String TAG = "Vuforia Navigation Sample";

    OpenGLMatrix lastLocation = null;

    /**
     * @see #captureFrameToFile()
     */
    int captureCounter = 0;
    File captureDirectory = AppUtil.ROBOT_DATA_DIR;

    /**
     * {@link #vuforia} is the variable we will use to store our instance of the Vuforia
     * localization engine.
     */
    VuforiaLocalizer vuforia;

    /**
     * This is the webcam we are to use. As with other hardware devices such as motors and
     * servos, this device is identified using the robot configuration tool in the FTC application.
     */
    WebcamName webcamName;

    @Override public void runOpMode() {

        /*
         * Retrieve the camera we are to use.
         */
        webcamName = hardwareMap.get(WebcamName.class, "Webcam 1");

        /*
         * To start up Vuforia, tell it the view that we wish to use for camera monitor (on the RC phone);
         * If no camera monitor is desired, use the parameterless constructor instead (commented out below).
         */
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);

        // OR...  Do Not Activate the Camera Monitor View, to save power
        // VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = "AWOqkCj/////AAABmbZveKODzktMn2KgOfwVvMJttzt42+YixY2fNhyAEPSMEqPT3e+Y5BiVmS+VjDEKa2ytk6Xin7nGHVPOGnA7QwDpiUaz1YsJZ/sz4SVQyQ6KV/4mGlwYNkpRUDSt2LDkpQHg2vqCOgBAU/rriU/YZgIgz5mti7cRYpo0NUqsE7qQO8rEFkIaBf+kJd1iO6ohVTpABImEKQnqUrQfupDRz/pR+ImpAwfzLzEWUjHDZYL/WGSyoJDKPuX0PDNtRBrFpvOVvlxmqULRDmP4kRSwnT5ZQBup9psUw7I00fkCHIO9QEmf6yiVosHQ4t4r+W1gYkJqIo94XZQpEP51fRzV439GXfj9ld0qrGds+KX/J9XS";

        /**
         * We also indicate which camera on the RC we wish to use.
         */
        parameters.cameraName = webcamName;

        /**
         * Instantiate the Vuforia engine
         */
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        /**
         * Because this opmode processes frames in order to write them to a file, we tell Vuforia
         * that we want to ensure that certain frame formats are available in the {@link Frame}s we
         * see.
         */
        vuforia.enableConvertFrameToBitmap();

        /** @see #captureFrameToFile() */
        AppUtil.getInstance().ensureDirectoryExists(captureDirectory);


        /**
         * Load the data sets that for the trackable objects we wish to track. These particular data
         * sets are stored in the 'assets' part of our application (you'll see them in the Android
         * Studio 'Project' view over there on the left of the screen).
         *     <ImageTarget name="red_perimeter_target_1" size="102.000000 67.511253" />
         *     <ImageTarget name="rear_perimeter_target_2" size="102.000000 57.375000" />
         *
         *     <ImageTarget name="rear_perimeter_target_1" size="102.000000 67.969093" />
         *     <ImageTarget name="front_perimeter_target_2" size="102.000000 41.564999" />
         *
         *     <ImageTarget name="front_perimeter_target_1" size="102.000000 68.063789" />
         *     <ImageTarget name="blue_perimeter_target_2" size="102.000000 42.703999" />
         *
         *     <ImageTarget name="blue_perimeter_target_1" size="102.000000 68.021248" />
         *     <ImageTarget name="red_perimeter_target_2" size="101.599899 67.690933" />
         */


        //Sets the reference directory
        VuforiaTrackables SkystoneImages = vuforia.loadTrackablesFromAsset("SkystoneImages");

        // Loads the images in
        VuforiaTrackable red_perimeter_target_1 = SkystoneImages.get(0);
        red_perimeter_target_1.setName("red_perimeter_target_1");

        VuforiaTrackable rear_perimeter_target_2  = SkystoneImages.get(1);
        rear_perimeter_target_2.setName("rear_perimeter_target_2");

        VuforiaTrackable rear_perimeter_target_1  = SkystoneImages.get(2);
        rear_perimeter_target_1.setName("rear_perimeter_target_1");

        VuforiaTrackable front_perimeter_target_2  = SkystoneImages.get(3);
        front_perimeter_target_2.setName("front_perimeter_target_2");

        VuforiaTrackable front_perimeter_target_1  = SkystoneImages.get(4);
        front_perimeter_target_1.setName("front_perimeter_target_1");

        VuforiaTrackable blue_perimeter_target_2  = SkystoneImages.get(5);
        blue_perimeter_target_2.setName("blue_perimeter_target_2");

        VuforiaTrackable blue_perimeter_target_1  = SkystoneImages.get(6);
        blue_perimeter_target_1.setName("blue_perimeter_target_1");

        VuforiaTrackable red_perimeter_target_2  = SkystoneImages.get(7);
        red_perimeter_target_2.setName("red_perimeter_target_2");




        /** For convenience, gather together all the trackable objects in one easily-iterable collection */
        List<VuforiaTrackable> allTrackables = new ArrayList<VuforiaTrackable>();
        allTrackables.addAll(SkystoneImages);

        /**
         * We use units of mm here because that's the recommended units of measurement for the
         * size values specified in the XML for the ImageTarget trackables in data sets. E.g.:
         *      <ImageTarget name="stones" size="247 173"/>
         * You don't *have to* use mm here, but the units here and the units used in the XML
         * target configuration files *must* correspond for the math to work out correctly.
         */
        float mmPerInch        = 25.4f;
        float mmBotWidth       = 18 * mmPerInch;            // ... or whatever is right for your robot
        float mmFTCFieldWidth  = (12*12 - 2) * mmPerInch;   // the FTC field is ~11'10" center-to-center of the glass panels

        /**
         * In order for localization to work, we need to tell the system where each target we
         * wish to use for navigation resides on the field, and we need to specify where on the robot
         * the camera resides. These specifications are in the form of <em>transformation matrices.</em>
         * Transformation matrices are a central, important concept in the math here involved in localization.
         * See <a href="https://en.wikipedia.org/wiki/Transformation_matrix">Transformation Matrix</a>
         * for detailed information. Commonly, you'll encounter transformation matrices as instances
         * of the {@link OpenGLMatrix} class.
         *
         * For the most part, you don't need to understand the details of the math of how transformation
         * matrices work inside (as fascinating as that is, truly). Just remember these key points:
         * <ol>
         *
         *     <li>You can put two transformations together to produce a third that combines the effect of
         *     both of them. If, for example, you have a rotation transform R and a translation transform T,
         *     then the combined transformation matrix RT which does the rotation first and then the translation
         *     is given by {@code RT = T.multiplied(R)}. That is, the transforms are multiplied in the
         *     <em>reverse</em> of the chronological order in which they applied.</li>
         *
         *     <li>A common way to create useful transforms is to use methods in the {@link OpenGLMatrix}
         *     class and the Orientation class. See, for example, {@link OpenGLMatrix#translation(float,
         *     float, float)}, {@link OpenGLMatrix#rotation(AngleUnit, float, float, float, float)}, and
         *     {@link Orientation#getRotationMatrix(AxesReference, AxesOrder, AngleUnit, float, float, float)}.
         *     Related methods in {@link OpenGLMatrix}, such as {@link OpenGLMatrix#rotated(AngleUnit,
         *     float, float, float, float)}, are syntactic shorthands for creating a new transform and
         *     then immediately multiplying the receiver by it, which can be convenient at times.</li>
         *
         *     <li>If you want to break open the black box of a transformation matrix to understand
         *     what it's doing inside, use {@link MatrixF#getTranslation()} to fetch how much the
         *     transform will move you in x, y, and z, and use {@link Orientation#getOrientation(MatrixF,
         *     AxesReference, AxesOrder, AngleUnit)} to determine the rotational motion that the transform
         *     will impart. See {@link #format(OpenGLMatrix)} below for an example.</li>
         *
         * </ol>
         *
         * This example places the "stones" image on the perimeter wall to the Left
         *  of the Red Driver station wall.  Similar to the Red Beacon Location on the Res-Q
         *
         * This example places the "chips" image on the perimeter wall to the Right
         *  of the Blue Driver station.  Similar to the Blue Beacon Location on the Res-Q
         *
         * See the doc folder of this project for a description of the Field Coordinate System
         * conventions.
         *
         * Initially the target is conceptually lying at the origin of the Field Coordinate System
         * (the center of the field), facing up.
         *
         * In this configuration, the target's coordinate system aligns with that of the field.
         *
         * In a real situation we'd also account for the vertical (Z) offset of the target,
         * but for simplicity, we ignore that here; for a real robot, you'll want to fix that.
         *
         * To place the Stones Target on the Red Audience wall:
         * - First we rotate it 90 around the field's X axis to flip it upright
         * - Then we rotate it  90 around the field's Z access to face it away from the audience.
         * - Finally, we translate it back along the X axis towards the red audience wall.
         */

        /*
         VuforiaTrackable red_perimeter_target_1 = SkystoneImages.get(0);
        red_perimeter_target_1.setName("red_perimeter_target_1");

        VuforiaTrackable rear_perimeter_target_2  = SkystoneImages.get(1);
        rear_perimeter_target_2.setName("rear_perimeter_target_2");

        VuforiaTrackable rear_perimeter_target_1  = SkystoneImages.get(2);
        rear_perimeter_target_1.setName("rear_perimeter_target_1");

        VuforiaTrackable front_perimeter_target_2  = SkystoneImages.get(3);
        front_perimeter_target_2.setName("front_perimeter_target_2");

        VuforiaTrackable front_perimeter_target_1  = SkystoneImages.get(4);
        front_perimeter_target_1.setName("front_perimeter_target_1");

        VuforiaTrackable blue_perimeter_target_2  = SkystoneImages.get(5);
        blue_perimeter_target_2.setName("blue_perimeter_target_2");

        VuforiaTrackable blue_perimeter_target_1  = SkystoneImages.get(6);
        blue_perimeter_target_1.setName("blue_perimeter_target_1");

        VuforiaTrackable red_perimeter_target_2  = SkystoneImages.get(7);
        red_perimeter_target_2.setName("red_perimeter_target_2");
         */




        OpenGLMatrix redTarget1LocationOnField = OpenGLMatrix
                /* Then we translate the target off to the RED WALL. Our translation here
                is a negative translation in X.*/
                .translation(-mmFTCFieldWidth/2, 0, 0)
                .multiplied(Orientation.getRotationMatrix(
                        /* First, in the fixed (field) coordinate system, we rotate 90deg in X, then 90 in Z */
                        AxesReference.EXTRINSIC, AxesOrder.XZX,
                        AngleUnit.DEGREES, 90, 90, 0));
        // Make sure to set this var to the one defined with xml
        red_perimeter_target_1.setLocationFtcFieldFromTarget(redTarget1LocationOnField);
        RobotLog.ii(TAG, "red_perimeter_target_1=%s", format(redTarget1LocationOnField));



        // comment out this block until later work is done
             /*
             * To place the Stones Target on the Blue Audience wall:
             * - First we rotate it 90 around the field's X axis to flip it upright
             * - Finally, we translate it along the Y axis towards the blue audience wall.
             */

        //   OpenGLMatrix blueTargetLocationOnField = OpenGLMatrix
        //           /* Then we translate the target off to the Blue Audience wall.
        //           Our translation here is a positive translation in Y.*/
        //           .translation(0, mmFTCFieldWidth/2, 0)
        //           .multiplied(Orientation.getRotationMatrix(
        //                    /* First, in the fixed (field) coordinate system, we rotate 90deg in X */
        //                    AxesReference.EXTRINSIC, AxesOrder.XZX,
        //                    AngleUnit.DEGREES, 90, 0, 0));
        //    blueTarget.setLocationFtcFieldFromTarget(blueTargetLocationOnField);
        //    RobotLog.ii(TAG, "Blue Target=%s", format(blueTargetLocationOnField));

        // this is just because comments don't work for some reason


        /**
         * We also need to tell Vuforia where the <em>cameras</em> are relative to the robot.
         *
         * Just as there is a Field Coordinate System, so too there is a Robot Coordinate System.
         * The two share many similarities. The origin of the Robot Coordinate System is wherever
         * you choose to make it on the robot, but typically you'd choose somewhere in the middle
         * of the robot. From that origin, the Y axis is horizontal and positive out towards the
         * "front" of the robot (however you choose "front" to be defined), the X axis is horizontal
         * and positive out towards the "right" of the robot (i.e.: 90deg horizontally clockwise from
         * the positive Y axis), and the Z axis is vertical towards the sky.
         *
         * Similarly, for each camera there is a Camera Coordinate System. The origin of a Camera
         * Coordinate System lies in the middle of the sensor inside of the camera. The Z axis is
         * positive coming out of the lens of the camera in a direction perpendicular to the plane
         * of the sensor. When looking at the face of the lens of the camera (down the positive Z
         * axis), the X axis is positive off to the right in the plane of the sensor, and the Y axis
         * is positive out the top of the lens in the plane of the sensor at 90 horizontally
         * counter clockwise from the X axis.
         *
         * Next, there is Phone Coordinate System (for robots that have phones, of course), though
         * with the advent of Vuforia support for Webcams, this coordinate system is less significant
         * than it was previously. The Phone Coordinate System is defined thusly: with the phone in
         * flat front of you in portrait mode (i.e. as it is when running the robot controller app)
         * and you are staring straight at the face of the phone,
         *     * X is positive heading off to your right,
         *     * Y is positive heading up through the top edge of the phone, and
         *     * Z is pointing out of the screen, toward you.
         * The origin of the Phone Coordinate System is at the origin of the Camera Coordinate System
         * of the front-facing camera on the phone.
         *
         * Finally, it is worth noting that trackable Vuforia Image Targets have their <em>own</em>
         * coordinate system (see {@link VuforiaTrackable}. This is sometimes referred to as the
         * Target Coordinate System. In keeping with the above, when looking at the target in its
         * natural orientation, in the Target Coodinate System
         *     * X is positive heading off to your right,
         *     * Y is positive heading up through the top edge of the target, and
         *     * Z is pointing out of the target, toward you.
         *
         * One can observe that the Camera Coordinate System of the front-facing camera on a phone
         * coincides with the Phone Coordinate System. Further, when a phone is placed on its back
         * at the origin of the Robot Coordinate System and aligned appropriately, those coordinate
         * systems also coincide with the Robot Coordinate System. Got it?
         *
         * In this example here, we're going to assume that we put the camera on the right side
         * of the robot (facing outwards, of course). To determine the transformation matrix that
         * describes that location, first consider the camera as lying on its back at the origin
         * of the Robot Coordinate System such that the Camera Coordinate System and Robot Coordinate
         * System coincide. Then the transformation we need is
         *      * first a rotation of the camera by +90deg along the robot X axis,
         *      * then a rotation of the camera by +90deg along the robot Z axis, and
         *      * finally a translation of the camera to the side of the robot.
         *
         * When determining whether a rotation is positive or negative, consider yourself as looking
         * down the (positive) axis of rotation from the positive towards the origin. Positive rotations
         * are then CCW, and negative rotations CW. An example: consider looking down the positive Z
         * axis towards the origin. A positive rotation about Z (ie: a rotation parallel to the the X-Y
         * plane) is then CCW, as one would normally expect from the usual classic 2D geometry.
         */


        // TODO: Make this actually work for the bot
        OpenGLMatrix robotFromCamera = OpenGLMatrix
                .translation(mmBotWidth/2,0,0)
                .multiplied(Orientation.getRotationMatrix(
                        AxesReference.EXTRINSIC, AxesOrder.XZY,
                        AngleUnit.DEGREES, 90, 90, 0));
        RobotLog.ii(TAG, "camera=%s", format(robotFromCamera));

        /**
         * Let the trackable listeners we care about know where the camera is. We know that each
         * listener is a {@link VuforiaTrackableDefaultListener} and can so safely cast because
         * we have not ourselves installed a listener of a different type.
         */
        //TODO: Update this for all the tracklable thingys
        ((VuforiaTrackableDefaultListener)red_perimeter_target_1.getListener()).setCameraLocationOnRobot(parameters.cameraName, robotFromCamera);
        //((VuforiaTrackableDefaultListener)blueTarget.getListener()).setCameraLocationOnRobot(parameters.cameraName, robotFromCamera);

        /**
         * A brief tutorial: here's how all the math is going to work:
         *
         * C = robotFromCamera          maps   camera coords -> robot coords
         * P = tracker.getPose()        maps   image target coords -> camera coords
         * L = redTargetLocationOnField maps   image target coords -> field coords
         *
         * So
         *
         * C.inverted()                 maps   robot coords -> camera coords
         * P.inverted()                 maps   camera coords -> imageTarget coords
         *
         * Putting that all together,
         *
         * L x P.inverted() x C.inverted() maps robot coords to field coords.
         *
         * @see VuforiaTrackableDefaultListener#getRobotLocation()
         */

        /** Wait for the game to begin */
        telemetry.addData(">", "Press Play to start tracking");
        telemetry.update();
        waitForStart();

        /** Start tracking the data sets we care about. */
        SkystoneImages.activate();

        boolean buttonPressed = false;
        while (opModeIsActive()) {

            if (gamepad1.a && !buttonPressed) {
                captureFrameToFile();
            }
            buttonPressed = gamepad1.a;

            for (VuforiaTrackable trackable : allTrackables) {
                /**
                 * getUpdatedRobotLocation() will return null if no new information is available since
                 * the last time that call was made, or if the trackable is not currently visible.
                 * getRobotLocation() will return null if the trackable is not currently visible.
                 */
                telemetry.addData(trackable.getName(), ((VuforiaTrackableDefaultListener)trackable.getListener()).isVisible() ? "Visible" : "Not Visible");    //

                OpenGLMatrix robotLocationTransform = ((VuforiaTrackableDefaultListener)trackable.getListener()).getUpdatedRobotLocation();
                if (robotLocationTransform != null) {
                    lastLocation = robotLocationTransform;
                }
            }
            /**
             * Provide feedback as to where the robot was last located (if we know).
             */
            if (lastLocation != null) {
                //  RobotLog.vv(TAG, "robot=%s", format(lastLocation));
                telemetry.addData("Pos", format(lastLocation));
            } else {
                telemetry.addData("Pos", "Unknown");
            }
            telemetry.update();
        }
    }


    /**
     * A simple utility that extracts positioning information from a transformation matrix
     * and formats it in a form palatable to a human being.
     */
    String format(OpenGLMatrix transformationMatrix) {
        return transformationMatrix.formatAsTransform();
    }

    /**
     * Sample one frame from the Vuforia stream and write it to a .PNG image file on the robot
     * controller in the /sdcard/FIRST/data directory. The images can be downloaded using Android
     * Studio's Device File Explorer, ADB, or the Media Transfer Protocol (MTP) integration into
     * Windows Explorer, among other means. The images can be useful during robot design and calibration
     * in order to get a sense of what the camera is actually seeing and so assist in camera
     * aiming and alignment.
     */
    void captureFrameToFile() {
        vuforia.getFrameOnce(Continuation.create(ThreadPool.getDefault(), new Consumer<Frame>()
        {
            @Override public void accept(Frame frame)
            {
                Bitmap bitmap = vuforia.convertFrameToBitmap(frame);
                if (bitmap != null) {
                    File file = new File(captureDirectory, String.format(Locale.getDefault(), "VuforiaFrame-%d.png", captureCounter++));
                    try {
                        FileOutputStream outputStream = new FileOutputStream(file);
                        try {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        } finally {
                            outputStream.close();
                            telemetry.log().add("captured %s", file.getName());
                        }
                    } catch (IOException e) {
                        RobotLog.ee(TAG, e, "exception in captureFrameToFile()");
                    }
                }
            }
        }));
    }
}