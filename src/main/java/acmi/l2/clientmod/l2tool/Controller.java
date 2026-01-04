/*
 * Copyright (c) 2016 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.l2tool;

import acmi.l2.clientmod.io.*;
import acmi.l2.clientmod.io.RandomAccessFile;
import acmi.l2.clientmod.l2tool.img.*;

import acmi.l2.clientmod.l2tool.img.MipMapInfo;
import acmi.l2.clientmod.l2tool.img.TextureProperties;
import acmi.l2.clientmod.texconv.ConvertTool;
import acmi.util.AutoCompleteComboBox;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.control.Separator;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import javax.imageio.ImageIO;
import java.util.HashSet;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static acmi.l2.clientmod.io.BufferUtil.getCompactInt;
import static javafx.collections.FXCollections.sort;

public class Controller implements Initializable {
    private static final String KEY_UTX_INITIAL_DIRECTORY = "utx_initial_directory";
    private static final String KEY_UED_INITIAL_DIRECTORY = "ued_initial_directory";
    private static final String KEY_IMAGE_INITIAL_DIRECTORY = "image_initial_directory";
    private static final String KEY_EXPORT_INITIAL_DIRECTORY = "export_initial_directory";

    private L2Tool application;

    private final StringProperty imgInitialDirectory = new SimpleStringProperty(
            L2Tool.getPrefs().get(KEY_IMAGE_INITIAL_DIRECTORY, null));
    @FXML
    private TextField imgPath;
    private final ObjectProperty<Img> imgProperty = new SimpleObjectProperty<>();

    private final StringProperty utxInitialDirectory = new SimpleStringProperty(
            L2Tool.getPrefs().get(KEY_UTX_INITIAL_DIRECTORY, null));
    private final StringProperty utxPathProperty = new SimpleStringProperty();
    @FXML
    private TextField utxPath;
    @FXML
    private ComboBox<MipMapInfo> textureList;
    @FXML
    private Button textureInfo;
    @FXML
    private Button toUED;
    @FXML
    private Button set;
    @FXML
    private final ObjectProperty<MipMapInfo> textureInfoProperty = new SimpleObjectProperty<>();
    @FXML
    private Button export;
    private final StringProperty exportInitialDirectory = new SimpleStringProperty(
            L2Tool.getPrefs().get(KEY_EXPORT_INITIAL_DIRECTORY, null));
    private final StringProperty uedInitialDirectory = new SimpleStringProperty(
            L2Tool.getPrefs().get(KEY_UED_INITIAL_DIRECTORY, null));
    @FXML
    private Button view;
    @FXML
    private Button exportAll;
    @FXML
    private CheckBox keepStructure;
    @FXML
    private CheckBox clearOutput;
    @FXML
    private RadioButton formatPNG;
    @FXML
    private RadioButton formatJPEG;
    @FXML
    private RadioButton formatBMP;
    @FXML
    private RadioButton formatGIF;
    @FXML
    private RadioButton formatWEBP;
    @FXML
    private RadioButton formatDDS;
    private ToggleGroup formatGroup;

    @FXML
    private ProgressIndicator progress;
    @FXML
    private HBox titleBar;
    @FXML
    private Button minimizeBtn;
    @FXML
    private Button closeBtn;

    private static final Set<Img.Format> SUPPORTED_FORMATS = new HashSet<Img.Format>() {
        {
            add(Img.Format.RGBA8);
            add(Img.Format.DXT1);
            add(Img.Format.DXT3);
            add(Img.Format.DXT5);
            add(Img.Format.G16);
            add(Img.Format.P8);
            // Nuevos formatos soportados
            add(Img.Format.RGB8);
            add(Img.Format.L8);
            add(Img.Format.RGBA7);
            add(Img.Format.RGB16);
            add(Img.Format.RRRGGGBBB);
        }
    };

    // private Dialog<Void> textureViewDialog;
    // private ImageView textureViewImage;
    // private Future loadImageTaskFuture;
    private MipMapInfo lastSelectedTexture;

    private double xOffset = 0;
    private double yOffset = 0;

    public void setApplication(L2Tool application) {
        this.application = application;

        if (titleBar != null) {
            titleBar.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });

            titleBar.setOnMouseDragged(event -> {
                application.getStage().setX(event.getScreenX() - xOffset);
                application.getStage().setY(event.getScreenY() - yOffset);
            });
        }
    }

    @FXML
    private void minimizeWindow() {
        if (application != null) {
            application.getStage().setIconified(true);
        }
    }

    @FXML
    private void closeWindow() {
        if (application != null) {
            application.getStage().close();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("LOG: Initializing controller...");
        System.out.println("LOG: Button injection check: view=" + (view != null) + ", info=" + (textureInfo != null));

        progress.setVisible(false);

        utxPath.textProperty().bind(utxPathProperty);
        textureList.disableProperty().bind(utxPathProperty.isNull());

        // NO BINDING for view/info buttons to keep them enabled for debug
        view.setDisable(false);
        textureInfo.setDisable(false);
        export.setDisable(false);
        set.setDisable(false);

        // Listener de selección
        textureList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lastSelectedTexture = newVal;
                System.out.println("LOG: lastSelectedTexture UPDATED to: " + newVal.name);
            } else if (lastSelectedTexture != null) {
                // Si el combo se limpia visualmente (foco, búsqueda),
                // restauramos internamente para que View/Info/Export sigan funcionando
                System.out.println("LOG: Selection visually lost, keeping persistent: " + lastSelectedTexture.name);
            }
        });

        // Corrección visual: Restaurar el valor si desaparece al perder el foco
        textureList.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && textureList.getValue() == null && lastSelectedTexture != null) {
                Platform.runLater(() -> {
                    textureList.setValue(lastSelectedTexture);
                    System.out.println("LOG: Visual selection restored on focus loss.");
                });
            }
        });

        // Corregir ClassCastException del AutoComplete
        textureList.setConverter(new javafx.util.StringConverter<MipMapInfo>() {
            @Override
            public String toString(MipMapInfo object) {
                return object == null ? "" : object.name;
            }

            @Override
            public MipMapInfo fromString(String string) {
                return null;
            }
        });

        exportAll.disableProperty().bind(utxPathProperty.isNull());

        formatGroup = new ToggleGroup();
        formatPNG.setToggleGroup(formatGroup);
        formatJPEG.setToggleGroup(formatGroup);
        formatBMP.setToggleGroup(formatGroup);
        formatGIF.setToggleGroup(formatGroup);
        formatWEBP.setToggleGroup(formatGroup);
        formatDDS.setToggleGroup(formatGroup);
        formatPNG.setSelected(true);

        try {
            Class.forName("io.github.gotson.webp.WebPImageWriterSpi");
        } catch (ClassNotFoundException e) {
            formatWEBP.setDisable(true);
            formatWEBP.setTooltip(new Tooltip("WebP support requires webp-imageio library"));
        }

        utxPathProperty.addListener((observableValue, oldPackagePath, newPackagePath) -> {
            System.out.println("LOG: Package path changed to: " + newPackagePath);
            textureList.getSelectionModel().clearSelection();
            textureList.getItems().clear();

            ForkJoinPool.commonPool().execute(() -> {
                Platform.runLater(() -> {
                    System.out.println("LOG: Starting to load package contents...");
                    progress.setProgress(0);
                    progress.setVisible(true);
                });

                try (UnrealPackage up = new UnrealPackage(newPackagePath, true)) {
                    up.getNameTable();
                    up.getImportTable();
                    int exportSize = up.getExportTable().size();

                    AtomicInteger counter = new AtomicInteger();
                    for (UnrealPackage.ExportEntry ee : up.getExportTable()) {
                        if (ee.getObjectClass() != null)
                            switch (ee.getObjectClass().getObjectFullName()) {
                                case "Engine.Texture": {
                                    MipMapInfo info = MipMapInfo.getInfo(ee);
                                    if (SUPPORTED_FORMATS.contains(info.format)) {
                                        if (info.offsets.length > 0)
                                            Platform.runLater(() -> textureList.getItems().add(info));
                                    }
                                    break;
                                }
                                case "Engine.GFxFlash": {
                                    byte[] raw = ee.getObjectRawData();
                                    ByteBuffer data = ByteBuffer.wrap(raw);

                                    new TextureProperties().read(up, data);

                                    String ext = up.nameReference(getCompactInt(data)).toLowerCase();
                                    switch (ext) {
                                        case "tga": {
                                            final MipMapInfo info = new MipMapInfo();
                                            info.exportIndex = ee.getIndex();
                                            info.name = ee.getObjectFullName();

                                            int dataLength = getCompactInt(data);

                                            // initial header fields
                                            int idLength = data.get() & 0xff;
                                            int colorMapType = data.get() & 0xff;
                                            int imageType = data.get() & 0xff;

                                            // color map header fields
                                            int firstEntryIndex = data.getShort() & 0xffff;
                                            int colorMapLength = data.getShort() & 0xffff;
                                            byte colorMapEntrySize = data.get();

                                            // TGA image specification fields
                                            int xOrigin = data.getShort() & 0xffff;
                                            int yOrigin = data.getShort() & 0xffff;
                                            int width = data.getShort() & 0xffff;
                                            int height = data.getShort() & 0xffff;
                                            byte pixelDepth = data.get();
                                            byte imageDescriptor = data.get();

                                            info.format = Img.Format.RGBA8;
                                            info.width = width;
                                            info.height = height;
                                            info.offsets = new int[] { data.position() };
                                            info.sizes = new int[] { raw.length - data.position() };

                                            if (info.offsets.length > 0)
                                                Platform.runLater(() -> textureList.getItems().add(info));
                                            break;
                                        }
                                        case "dds": {
                                            final MipMapInfo info = new MipMapInfo();
                                            info.exportIndex = ee.getIndex();
                                            info.name = ee.getObjectFullName();

                                            byte[] dds = new byte[getCompactInt(data)];
                                            int dataOffset = data.position();
                                            data.get(dds);
                                            ByteBuffer buffer = ByteBuffer.wrap(dds);
                                            DDSImage image = DDSImage.read(buffer);
                                            info.format = DDS.getFormat(image.getCompressionFormat());
                                            info.width = image.getWidth();
                                            info.height = image.getHeight();
                                            DDSImage.ImageInfo[] infos = image.getAllMipMaps();
                                            info.offsets = new int[infos.length];
                                            info.sizes = new int[infos.length];
                                            info.offsets[0] = 128 + dataOffset;
                                            info.sizes[0] = infos[0].getData().limit();
                                            for (int i = 1; i < infos.length; i++) {
                                                info.offsets[i] = info.offsets[i - 1] + info.sizes[i - 1];
                                                info.sizes[i] = infos[i].getData().limit();
                                            }
                                            Platform.runLater(() -> textureList.getItems().add(info));
                                            break;
                                        }
                                    }

                                    break;
                                }
                                default:
                                    // ignore
                            }

                        Platform.runLater(() -> progress.setProgress((double) counter.incrementAndGet() / exportSize));
                    }

                    Platform.runLater(() -> {
                        System.out.println("LOG: Sorting " + textureList.getItems().size() + " textures...");
                        sort(textureList.getItems(),
                                (o1, o2) -> o1.name.toLowerCase().compareTo(o2.name.toLowerCase()));
                        System.out.println("LOG: Texture list updated and sorted.");
                    });
                } catch (final Exception e) {
                    System.out.println("LOG: ERROR loading package: " + e.getMessage());
                    e.printStackTrace(System.out);
                    Platform.runLater(() -> showError(e));
                } finally {
                    Platform.runLater(() -> {
                        progress.setVisible(false);
                        System.out.println("LOG: Progress hidden.");
                    });
                }
            });
        });

        AutoCompleteComboBox.autoCompleteComboBox(textureList,
                AutoCompleteComboBox.AutoCompleteMode.CONTAINING);
        System.out.println("LOG: AutoCompleteComboBox initialized.");

        textureInfoProperty.bind(Bindings.createObjectBinding(() -> {
            MipMapInfo selected = AutoCompleteComboBox.getSelectedItem(textureList);
            System.out.println("LOG: Binding eval: " + (selected != null ? selected.name : "NULL"));
            return selected;
        }, textureList.getSelectionModel().selectedIndexProperty()));

        textureInfoProperty.addListener((obs, oldV, newV) -> {
            System.out.println("LOG: Selection changed to: " + (newV != null ? newV.name : "NULL"));
            if (newV != null) {
                lastSelectedTexture = newV;
                System.out.println("LOG: lastSelectedTexture UPDATED to: " + newV.name);
            }
        });

        // Asegurar que los botones respondan incluso si el FXML falla
        textureInfo.setOnAction(e -> showTextureInfo());
        view.setOnAction(e -> showTexture());

        System.out.println("LOG: Button listeners manually attached.");
    }

    @FXML
    private void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("DXT", "*.dds"),
                new FileChooser.ExtensionFilter("TGA", "*.tga"),
                new FileChooser.ExtensionFilter("BMP", "*.bmp"));
        if (imgInitialDirectory.get() != null) {
            File dir = new File(imgInitialDirectory.get());
            if (dir.exists() && dir.isDirectory())
                fileChooser.setInitialDirectory(dir);
        }

        File file = fileChooser.showOpenDialog(application.getStage());
        if (file == null)
            return;

        imgInitialDirectory.set(file.getParent());

        try {
            Img image;
            switch (file.getName().substring(file.getName().lastIndexOf('.') + 1)) {
                case "dds":
                    image = DDS.loadFromFile(file);
                    break;
                case "tga":
                    image = TGA.loadFromFile(file);
                    break;
                case "bmp":
                    try (RandomAccessFile raf = new RandomAccessFile(file, true, null)) {
                        raf.setPosition(0x1c);
                        int bpp = raf.readUnsignedShort();
                        switch (bpp) {
                            case 8:
                                image = P8.loadFromFile(file);
                                break;
                            case 16:
                                image = G16.loadFromFile(file);
                                break;
                            default:
                                throw new IOException(String.format("%d bit per pixel not supported", bpp));
                        }
                    }
                    break;
                default:
                    throw new IOException("Unknown file format");
            }
            imgProperty.setValue(image);
            imgPath.setText(file.getAbsolutePath() + "[" + image.getWidth() + "x" + image.getHeight() + ","
                    + image.getFormat() + "," + image.getMipMaps().length + "]");
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void selectPackage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Package");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Unreal package", "*.utx", "*.ugx"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        if (utxInitialDirectory.get() != null) {
            File file = new File(utxInitialDirectory.get());
            if (file.exists() && file.isDirectory())
                fileChooser.setInitialDirectory(file);
        }

        File file = fileChooser.showOpenDialog(application.getStage());
        if (file == null)
            return;

        utxInitialDirectory.set(file.getParent());

        try (UnrealPackage utxFile = new UnrealPackage(file, true)) {
            utxPathProperty.setValue(file.getAbsolutePath());
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void showTextureInfo() {
        System.out.println("LOG: showTextureInfo CLICKED (Unified UI)");
        MipMapInfo info = (textureInfoProperty.get() != null) ? textureInfoProperty.get() : lastSelectedTexture;

        if (info == null) {
            show(Alert.AlertType.WARNING, "Selection Required", null, "Please select a texture first.");
            return;
        }

        VBox content = new VBox(10);
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);

        addInfoRow(grid, 0, "Format:", info.format.toString());
        addInfoRow(grid, 1, "Dimensions:", info.width + " x " + info.height);
        addInfoRow(grid, 2, "MipMaps:", String.valueOf(info.offsets.length));
        addInfoRow(grid, 3, "Export Index:", String.valueOf(info.exportIndex));
        if (info.sizes != null && info.sizes.length > 0) {
            addInfoRow(grid, 4, "Data Size:", (info.sizes[0] / 1024) + " KB (Main Mip)");
        }
        content.getChildren().add(grid);

        showCustomDialog("Texture Details", content, "info");
    }

    private void addInfoRow(GridPane grid, int row, String label, String value) {
        Label lblKey = new Label(label);
        lblKey.setStyle("-fx-text-fill: #888; -fx-font-weight: bold;");
        Label lblVal = new Label(value);
        lblVal.setStyle("-fx-text-fill: #eee;");
        grid.add(lblKey, 0, row);
        grid.add(lblVal, 1, row);
    }

    private void updateTextureInfoLabel(MipMapInfo info) {
        // Stubbed to fix compilation
    }

    @FXML
    private void showTexture() {
        System.out.println("LOG: showTexture CLICKED");
        MipMapInfo info = (textureInfoProperty.get() != null) ? textureInfoProperty.get() : lastSelectedTexture;

        if (info == null) {
            System.out.println("LOG: showTexture FAILED - info is null");
            show(Alert.AlertType.WARNING, "Selection Required", null, "Please select a texture first.");
            return;
        }

        try {
            System.out.println("LOG: Loading texture data for " + info.name + "...");
            BufferedImage img = loadUtxImage(info);
            if (img == null) {
                System.out.println("LOG: Error - BufferedImage is null.");
                show(Alert.AlertType.ERROR, "Error", null, "Could not load image data.");
                return;
            }

            File tempFile = File.createTempFile("l2tool_view_" + info.name + "_", ".png");
            // Se elimina al salir de la App, pero no ahora
            // tempFile.deleteOnExit();

            ImageIO.write(img, "png", tempFile);
            System.out.println("LOG: Temp PNG created: " + tempFile.getAbsolutePath());

            if (Desktop.isDesktopSupported()) {
                System.out.println("LOG: Opening system viewer...");
                Desktop.getDesktop().open(tempFile);
            } else {
                System.out.println("LOG: Desktop not supported, showing info instead.");
                show(Alert.AlertType.INFORMATION, "View (Manual)", info.name,
                        "Texture saved to: " + tempFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("LOG: ERROR in showTexture");
            e.printStackTrace(System.out);
            showError(e);
        }
    }

    private BufferedImage loadUtxImage(MipMapInfo info) throws Exception {
        try (UnrealPackage utx = new UnrealPackage(new File(utxPathProperty.get()), true)) {
            UnrealPackage.ExportEntry texture = utx.getExportTable().get(info.exportIndex);
            byte[] raw = texture.getObjectRawData();
            switch (info.format) {
                case RGBA8:
                    return TGA.createFromData(raw, info).getMipMaps()[0];
                case DXT1:
                case DXT3:
                case DXT5:
                    return DDS.createFromData(raw, info).getMipMaps()[0];
                case G16:
                    return G16.createFromData(raw, info).getMipMaps()[0];
                case P8:
                    return P8.createFromData(raw, info).getMipMaps()[0];
                // Nuevos formatos
                case RGB8:
                case L8:
                case RGBA7:
                case RGB16:
                case RRRGGGBBB:
                    return GenericTexture.createFromData(raw, info).getMipMaps()[0];
                default:
                    throw new Exception("Unsupported format " + info.format);
            }
        }
    }

    @FXML
    private void exportTexture() {
        System.out.println("LOG: exportTexture CLICKED");
        MipMapInfo info = (textureInfoProperty.get() != null) ? textureInfoProperty.get() : lastSelectedTexture;

        if (info == null) {
            show(Alert.AlertType.WARNING, "Selection Required", null, "Please select a texture first.");
            return;
        }

        try (UnrealPackage utx = new UnrealPackage(new File(utxPathProperty.get()), true)) {
            File outputDir = new File("output_selected");

            if (clearOutput.isSelected() && outputDir.exists()) {
                try {
                    deleteDirectoryContents(outputDir);
                } catch (IOException e) {
                    show(Alert.AlertType.ERROR, "Error", null, "Could not clear output folder: " + e.getMessage());
                }
            }

            if (!outputDir.exists())
                outputDir.mkdirs();

            String format = "png";
            String extension = ".png";
            if (formatJPEG.isSelected()) {
                format = "jpg";
                extension = ".jpg";
            } else if (formatBMP.isSelected()) {
                format = "bmp";
                extension = ".bmp";
            } else if (formatGIF.isSelected()) {
                format = "gif";
                extension = ".gif";
            } else if (formatWEBP.isSelected()) {
                format = "webp";
                extension = ".webp";
            } else if (formatDDS.isSelected() && (info.format == Img.Format.DXT1 || info.format == Img.Format.DXT3
                    || info.format == Img.Format.DXT5)) {
                format = "dds";
                extension = ".dds";
            }

            String textureName;
            if (keepStructure.isSelected()) {
                UnrealPackage.ExportEntry entry = utx.getExportTable().get(info.exportIndex);
                textureName = fixPath(entry.getObjectFullName());
            } else {
                textureName = info.name;
            }

            File outputFile = new File(outputDir, textureName + extension);
            createParentDirectories(outputFile);

            System.out.println("LOG: Exporting " + info.name + " to " + outputFile.getAbsolutePath());

            if (format.equals("dds")) {
                UnrealPackage.ExportEntry texture = utx.getExportTable().get(info.exportIndex);
                DDS.createFromData(texture.getObjectRawData(), info).write(outputFile);
            } else {
                BufferedImage image = loadUtxImage(info);
                BufferedImage writeImage = image;
                if (format.equals("jpg") && writeImage.getType() != BufferedImage.TYPE_INT_RGB) {
                    BufferedImage rgbImage = new BufferedImage(writeImage.getWidth(), writeImage.getHeight(),
                            BufferedImage.TYPE_INT_RGB);
                    rgbImage.getGraphics().drawImage(image, 0, 0, null);
                    writeImage = rgbImage;
                }
                ImageIO.write(writeImage, format, outputFile);
            }

            show(Alert.AlertType.INFORMATION, "Success", null, "Texture exported to:\n" + outputFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("LOG: ERROR in exportTexture");
            e.printStackTrace(System.out);
            showError(e);
        }
    }

    @FXML
    private void replaceTexture() {
        MipMapInfo info = textureInfoProperty.get();
        Img data = imgProperty.get();

        if (info.format != data.getFormat()) {
            show(Alert.AlertType.WARNING, "Replace failed", null,
                    "img format:\t" + data.getFormat() + "\nutx format:\t" + info.format);
            return;
        }

        if (info.width != data.getWidth() || info.height != data.getHeight()) {
            show(Alert.AlertType.WARNING, "Replace failed", null,
                    "img size:\t" + data.getWidth() + "x" + data.getHeight() + "\nutx size:\t" + info.width + "x"
                            + info.height);
            return;
        }

        if (info.offsets.length > data.getMipMaps().length) {
            show(Alert.AlertType.WARNING, "Replace failed", null,
                    "img mipmap count:\t" + data.getMipMaps().length + "\nutx mipmap count:\t" + info.offsets.length);
            return;
        }

        try (UnrealPackage utx = new UnrealPackage(new File(utxPathProperty.get()), false)) {
            UnrealPackage.ExportEntry texture = utx.getExportTable().get(info.exportIndex);
            byte[] buffer = texture.getObjectRawData();
            for (int i = 0; i < info.offsets.length; i++) {
                byte[] replace = null;
                for (int j = 0; j < data.getMipMaps().length; j++) {
                    if (data.getData()[j].length == info.sizes[i])
                        replace = data.getData()[j];
                }
                if (replace == null)
                    throw new Exception("No suitable MipMap found");

                System.arraycopy(replace, 0, buffer, info.offsets[i], info.sizes[i]);
            }
            texture.setObjectRawData(buffer);

            if (info.format == Img.Format.P8) {
                P8 p8 = (P8) data;
                info.palette = p8.palette;
                info.palette.writeToUnrealPackage(utx);
            }
            show(Alert.AlertType.INFORMATION, "Success", null,
                    "Texture " + texture.toString() + " successfully replaced.");
        } catch (Exception e) {
            showError(e);
        }
    }

    @FXML
    private void exportAllTextures() {
        if (utxPathProperty.get() == null || utxPathProperty.get().isEmpty()) {
            show(Alert.AlertType.WARNING, "Error", null, "Please select a UTX file first.");
            return;
        }

        try {
            File utxFile = new File(utxPathProperty.get());
            File outputDir = new File("output_all");

            if (clearOutput.isSelected() && outputDir.exists()) {
                try {
                    deleteDirectoryContents(outputDir);
                } catch (IOException e) {
                    show(Alert.AlertType.ERROR, "Error", null, "Could not clear output_all folder: " + e.getMessage());
                    return;
                }
            }

            if (!outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    show(Alert.AlertType.ERROR, "Error", null, "Could not create the 'output_all' folder.");
                    return;
                }
            }

            progress.setProgress(-1);
            progress.setVisible(true);

            ForkJoinPool.commonPool().execute(() -> {
                int exported = 0;
                int errors = 0;

                try (UnrealPackage up = new UnrealPackage(utxFile, true)) {
                    for (UnrealPackage.ExportEntry entry : up.getExportTable()) {
                        if (entry.getObjectClass() == null) {
                            continue;
                        }

                        String objectClass = entry.getObjectClass().getObjectFullName();
                        BufferedImage image = null;
                        Img imgObject = null;
                        MipMapInfo info = null;

                        String textureName;
                        if (keepStructure.isSelected()) {
                            textureName = fixPath(entry.getObjectFullName());
                        } else {
                            String fullName = entry.getObjectFullName();
                            int lastDot = fullName.lastIndexOf('.');
                            textureName = lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
                        }

                        try {
                            // Procesar texturas tipo Engine.Texture
                            if (ConvertTool.isTexture(objectClass)) {
                                info = MipMapInfo.getInfo(entry);
                                if (!SUPPORTED_FORMATS.contains(info.format)) {
                                    continue;
                                }

                                // Validar que tenga mipmaps antes de procesar
                                if (info.sizes == null || info.sizes.length == 0 || info.offsets == null
                                        || info.offsets.length == 0) {
                                    errors++;
                                    continue;
                                }

                                byte[] raw = entry.getObjectRawData();

                                switch (info.format) {
                                    case DXT1:
                                    case DXT3:
                                    case DXT5:
                                        imgObject = DDS.createFromData(raw, info);
                                        image = imgObject.getMipMaps()[0];
                                        break;
                                    case RGBA8:
                                        imgObject = TGA.createFromData(raw, info);
                                        image = imgObject.getMipMaps()[0];
                                        break;
                                    case P8:
                                        // Validar que tenga mipmaps antes de procesar P8
                                        if (info.sizes.length > 0 && info.offsets.length > 0) {
                                            imgObject = P8.createFromData(raw, info);
                                            image = imgObject.getMipMaps()[0];
                                        }
                                        break;
                                    case G16:
                                        // Validar que tenga mipmaps antes de procesar G16
                                        if (info.sizes.length > 0 && info.offsets.length > 0) {
                                            imgObject = G16.createFromData(raw, info);
                                            image = imgObject.getMipMaps()[0];
                                        }
                                        break;
                                    // Nuevos formatos usando GenericTexture
                                    case RGB8:
                                    case L8:
                                    case RGBA7:
                                    case RGB16:
                                    case RRRGGGBBB:
                                        if (info.sizes.length > 0 && info.offsets.length > 0) {
                                            imgObject = GenericTexture.createFromData(raw, info);
                                            image = imgObject.getMipMaps()[0];
                                        }
                                        break;
                                    default:
                                        continue;
                                }
                            }
                            // Procesar texturas tipo Engine.GFxFlash
                            else if ("Engine.GFxFlash".equals(objectClass)) {
                                byte[] raw = entry.getObjectRawData();
                                ByteBuffer data = ByteBuffer.wrap(raw);

                                new TextureProperties().read(up, data);

                                String ext = up.nameReference(getCompactInt(data)).toLowerCase();
                                switch (ext) {
                                    case "tga": {
                                        int dataLength = getCompactInt(data);

                                        int idLength = data.get() & 0xff;
                                        int colorMapType = data.get() & 0xff;
                                        int imageType = data.get() & 0xff;

                                        int firstEntryIndex = data.getShort() & 0xffff;
                                        int colorMapLength = data.getShort() & 0xffff;
                                        byte colorMapEntrySize = data.get();

                                        int xOrigin = data.getShort() & 0xffff;
                                        int yOrigin = data.getShort() & 0xffff;
                                        int width = data.getShort() & 0xffff;
                                        int height = data.getShort() & 0xffff;
                                        byte pixelDepth = data.get();
                                        byte imageDescriptor = data.get();

                                        info = new MipMapInfo();
                                        info.exportIndex = entry.getIndex();
                                        info.name = entry.getObjectFullName();
                                        info.format = Img.Format.RGBA8;
                                        info.width = width;
                                        info.height = height;
                                        info.offsets = new int[] { data.position() };
                                        info.sizes = new int[] { raw.length - data.position() };

                                        if (info.offsets.length > 0) {
                                            imgObject = TGA.createFromData(raw, info);
                                            image = imgObject.getMipMaps()[0];
                                        }
                                        break;
                                    }
                                    case "dds": {
                                        byte[] dds = new byte[getCompactInt(data)];
                                        int dataOffset = data.position();
                                        data.get(dds);
                                        ByteBuffer buffer = ByteBuffer.wrap(dds);
                                        DDSImage ddsImage = DDSImage.read(buffer);

                                        info = new MipMapInfo();
                                        info.exportIndex = entry.getIndex();
                                        info.name = entry.getObjectFullName();
                                        info.format = DDS.getFormat(ddsImage.getCompressionFormat());
                                        info.width = ddsImage.getWidth();
                                        info.height = ddsImage.getHeight();
                                        DDSImage.ImageInfo[] infos = ddsImage.getAllMipMaps();
                                        info.offsets = new int[infos.length];
                                        info.sizes = new int[infos.length];
                                        info.offsets[0] = 128 + dataOffset;
                                        info.sizes[0] = infos[0].getData().limit();
                                        for (int i = 1; i < infos.length; i++) {
                                            info.offsets[i] = info.offsets[i - 1] + info.sizes[i - 1];
                                            info.sizes[i] = infos[i].getData().limit();
                                        }

                                        byte[] fullRaw = entry.getObjectRawData();
                                        imgObject = DDS.createFromData(fullRaw, info);
                                        image = imgObject.getMipMaps()[0];
                                        break;
                                    }
                                }
                            }

                            if (image != null && imgObject != null && info != null) {
                                File outputFile;

                                // Exportar como DDS si está seleccionado y la textura es DXT
                                if (formatDDS.isSelected() &&
                                        (info.format == Img.Format.DXT1 ||
                                                info.format == Img.Format.DXT3 ||
                                                info.format == Img.Format.DXT5)
                                        &&
                                        imgObject instanceof DDS) {
                                    outputFile = new File(outputDir, textureName + ".dds");
                                    createParentDirectories(outputFile);
                                    ((DDS) imgObject).write(outputFile);
                                    exported++;
                                } else {
                                    // Exportar usando ImageIO para otros formatos
                                    String format = "png";
                                    String extension = ".png";
                                    if (formatJPEG.isSelected()) {
                                        format = "jpg";
                                        extension = ".jpg";
                                    } else if (formatBMP.isSelected()) {
                                        format = "bmp";
                                        extension = ".bmp";
                                    } else if (formatGIF.isSelected()) {
                                        format = "gif";
                                        extension = ".gif";
                                    } else if (formatWEBP.isSelected()) {
                                        format = "webp";
                                        extension = ".webp";
                                    }

                                    outputFile = new File(outputDir, textureName + extension);
                                    createParentDirectories(outputFile);

                                    // Convertir BufferedImage a formato compatible con ImageIO
                                    BufferedImage writeImage = image;
                                    if (format.equals("jpg")) {
                                        // JPEG no soporta transparencia, convertir a RGB
                                        if (writeImage.getType() != BufferedImage.TYPE_INT_RGB) {
                                            BufferedImage rgbImage = new BufferedImage(writeImage.getWidth(),
                                                    writeImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                                            rgbImage.getGraphics().drawImage(image, 0, 0, null);
                                            writeImage = rgbImage;
                                        }
                                    } else if (format.equals("bmp")) {
                                        // Asegurar tipo estándar para BMP
                                        if (writeImage.getType() != BufferedImage.TYPE_INT_RGB &&
                                                writeImage.getType() != BufferedImage.TYPE_INT_ARGB) {
                                            BufferedImage newImage = new BufferedImage(writeImage.getWidth(),
                                                    writeImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                            newImage.getGraphics().drawImage(writeImage, 0, 0, null);
                                            writeImage = newImage;
                                        }
                                    }

                                    ImageIO.write(writeImage, format, outputFile);
                                    exported++;
                                }
                            }
                        } catch (Exception e) {
                            errors++;
                            e.printStackTrace();
                        }
                    }

                    final int finalExported = exported;
                    final int finalErrors = errors;
                    Platform.runLater(() -> {
                        progress.setVisible(false);
                        String message = "Export completed.\nTextures exported: " + finalExported;
                        if (finalErrors > 0) {
                            message += "\nErrors: " + finalErrors;
                        }
                        message += "\nFolder: " + outputDir.getAbsolutePath();
                        show(Alert.AlertType.INFORMATION, "Success", null, message);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        progress.setVisible(false);
                        showError(e);
                    });
                }
            });
        } catch (Exception e) {
            progress.setVisible(false);
            showError(e);
        }
    }

    private static String fixPath(String path) {
        while (path.indexOf('.') != -1) {
            path = path.replace(".", File.separator);
        }
        return path;
    }

    private static void createParentDirectories(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Could not create directory: " + parent.getAbsolutePath());
            }
        }
    }

    private static void deleteDirectoryContents(File directory) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryContents(file);
                }
                if (!file.delete()) {
                    // Continue with next file
                }
            }
        }
    }

    @FXML
    private void convertUTX() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save UTX");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("utx", "*.utx"));
        if (uedInitialDirectory.get() == null)
            uedInitialDirectory.set(utxInitialDirectory.get());
        File dir = new File(uedInitialDirectory.get());
        if (dir.exists() && dir.isDirectory())
            fileChooser.setInitialDirectory(dir);
        fileChooser.setInitialFileName(new File(utxPathProperty.get()).getName());

        final File file = fileChooser.showSaveDialog(application.getStage());
        if (file == null)
            return;

        uedInitialDirectory.set(file.getParent());

        String savePath = file.getAbsolutePath();
        if (!savePath.endsWith(".utx"))
            savePath += ".utx";
        final String savePath1 = savePath;
        progress.setProgress(-1);
        progress.setVisible(true);
        ForkJoinPool.commonPool().execute(() -> {
            try (UnrealPackage up = new UnrealPackage(new File(utxPathProperty.get()), true)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ConvertTool.save(up, new File(savePath1), new PrintStream(baos, true, "utf-16le"));
                Platform.runLater(() -> show(Alert.AlertType.INFORMATION, "Convert complete",
                        baos.size() == 0 ? null : "Changelist", baos.size() == 0 ? "No changes."
                                : new String(baos.toByteArray(), Charset.forName("utf-16le"))));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e));
            } finally {
                Platform.runLater(() -> progress.setVisible(false));
            }
        });
    }

    private void show(Alert.AlertType alertType, String title, String headerText, String contentText) {
        if (alertType == Alert.AlertType.INFORMATION || alertType == Alert.AlertType.CONFIRMATION) {
            VBox content = new VBox(5);
            if (headerText != null) {
                Label header = new Label(headerText);
                header.setStyle("-fx-text-fill: #00d2ff; -fx-font-weight: bold;");
                content.getChildren().add(header);
            }
            Label msg = new Label(contentText);
            msg.setStyle("-fx-text-fill: #eee;");
            msg.setWrapText(true);
            content.getChildren().add(msg);

            showCustomDialog(title, content, "success");
        } else {
            // Keep standard alert for Warnings/Errors for now or map them too
            Alert alert = new Alert(alertType);
            if (application != null && application.getStage() != null) {
                alert.initOwner(application.getStage());
            }
            alert.setTitle(title);
            alert.setHeaderText(headerText);
            alert.setContentText(contentText);
            alert.show();
        }
    }

    private double dialogX, dialogY;

    private void showCustomDialog(String titleStr, Parent content, String type) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initOwner(application.getStage());

        // Header (Title Bar)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 10, 5, 10));
        header.setStyle("-fx-background-color: #0f0f1a; -fx-cursor: move;");
        header.setPrefHeight(32);

        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("L2Tool.png")));
        icon.setFitWidth(16);
        icon.setFitHeight(16);

        Label titleLbl = new Label(titleStr);
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #888; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 10 0 10;");
        closeBtn.setPrefHeight(32);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 10 0 10;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #888; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 10 0 10;"));
        closeBtn.setOnAction(e -> stage.close());

        header.getChildren().addAll(icon, titleLbl, spacer, closeBtn);

        // Header Dragging
        header.setOnMousePressed(e -> {
            dialogX = e.getSceneX();
            dialogY = e.getSceneY();
        });
        header.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dialogX);
            stage.setY(e.getScreenY() - dialogY);
        });

        // Content Area
        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #303050; -fx-border-width: 0 1 1 1;");
        root.setPadding(new Insets(20));

        VBox mainContent = new VBox(10);
        mainContent.getChildren().add(content);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button("OK");
        okBtn.setPrefWidth(80);
        okBtn.setStyle(
                "-fx-background-color: #0f3460; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #1a5a8a; -fx-border-radius: 4; -fx-background-radius: 4;");
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(
                "-fx-background-color: #1a5a8a; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #2a6a9a; -fx-border-radius: 4; -fx-background-radius: 4;"));
        okBtn.setOnMouseExited(e -> okBtn.setStyle(
                "-fx-background-color: #0f3460; -fx-text-fill: white; -fx-cursor: hand; -fx-border-color: #1a5a8a; -fx-border-radius: 4; -fx-background-radius: 4;"));
        okBtn.setOnAction(e -> stage.close());
        footer.getChildren().add(okBtn);

        root.getChildren().addAll(mainContent, footer);

        VBox layout = new VBox(header, root);
        Scene scene = new Scene(layout);
        scene.setFill(null);
        stage.setScene(scene);

        // Position relative to main stage
        if (application.getStage() != null) {
            stage.setOnShown(e -> {
                stage.setX(application.getStage().getX() + (application.getStage().getWidth() - stage.getWidth()) / 2);
                stage.setY(
                        application.getStage().getY() + (application.getStage().getHeight() - stage.getHeight()) / 2);
            });
        }

        stage.show();
    }

    private void showError(Throwable t) {
        System.out.println("LOG: showError() called");
        t.printStackTrace(System.out);

        while (t.getCause() != null)
            t = t.getCause();

        show(Alert.AlertType.ERROR, t.getClass().getSimpleName(), null, trimMessage(t.getMessage()));
    }

    private String trimMessage(String s) {
        return s == null ? null : s.substring(0, Math.min(160, s.length()));
    }
}
