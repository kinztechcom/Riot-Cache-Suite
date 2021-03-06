package gg.raf.suite.fs.file.dds;

import gg.raf.suite.fs.file.RiotFile;
import gg.raf.suite.fs.file.dds.decompressor.DXT1Decompressor;
import gg.raf.suite.fs.file.dds.decompressor.DXT3Decompressor;
import gg.raf.suite.fs.file.dds.decompressor.DXT5Decompressor;
import gg.raf.suite.fs.file.dds.decompressor.Decompressor;
import gg.raf.suite.fs.file.dds.flags.DDSCaps2Flags;
import gg.raf.suite.fs.file.dds.flags.DDSCapsFlags;
import gg.raf.suite.fs.file.dds.flags.DDSHeaderFlags;
import gg.raf.suite.fs.file.dds.flags.DDSPixelFormatFlags;
import gg.raf.suite.utilities.StringUtil;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.awt.image.BufferedImage;
import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;

/**
 * Created by Allen Kinzalow on 9/30/2015.
 *
 * A DirectDraw Surface file format is a Microsoft format for
 *  storing data compressed with the proprietary S3 Texture Compression
 *  algorithm, which can be decompressed in hardware by GPUs.
 *  This makes the format useful for storing graphical textures and
 *  cubic environment maps as a data file, both compressed and uncompressed.
 * Source: https://en.wikipedia.org/wiki/DirectDraw_Surface
 *
 * A file representing a DDS texture file that contains the
 * necessary methods to decode the protocol.
 *
 * Source Reference: https://msdn.microsoft.com/en-us/library/bb943982(v=vs.85).aspx
 */
public class DDSFile extends RiotFile {

    /**
     * FourCC constants.
     */
    /**
     * Three color channels 5:6:5 with 0 or 1 bits of alpha
     */
    private static final int PF_DXT1 = 0x31545844;
    private static final int PF_DXT3 = 0x33545844;

    /**
     * Three color channels 5:6:5 with 8 bits alpha
     */
    private static final int PF_DXT5 = 0x35545844;

    /**
     * A "magic number" containing the four character code 'DDS' (0x20534444).
     */
    private int ddsMagicNumber;

    /**
     * A description of the data in this file.
     */
    private DDSHeader ddsHeader;

    /**
     * Is the dds file compressed?
     */
    private boolean compressed = false;

    /**
     * Is the texture either grayscale or alpha?
     */
    private boolean grayscaleOrAlpha;

    /**
     * An array of sizes of each mipmap level in bytes.
     */
    private int[] mipmapSizes;

    /**
     * The format of this dds texture.
     */
    private DDSFormat format;

    /**
     * The result of the dds decoding.
     */
    private BufferedImage result;

    public DDSFile(int hash, int dataOffset, int dataSize, int pathListIndex) {
        super(hash, dataOffset, dataSize, pathListIndex);
        ddsHeader = new DDSHeader();
    }

    public DDSFile(RiotFile file) {
        super(file);
        ddsHeader = new DDSHeader();
    }

    @Override
    public void decode() {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(this.getFileData());

            buffer.order(ByteOrder.LITTLE_ENDIAN);

            this.ddsMagicNumber = buffer.getInt();
            System.out.println("Magic: " + ddsMagicNumber);
            ddsHeader.setDwSize(buffer.getInt());
            ddsHeader.setDwFlags(buffer.getInt());
            System.out.println("Size: " + ddsHeader.getDwSize() + " Flags: " + ddsHeader.getDwFlags());
            ddsHeader.populateFlags();
            ddsHeader.setDwWidth(buffer.getInt());
            ddsHeader.setDwHeight(buffer.getInt());
            ddsHeader.setDwPitchOrLinearSize(buffer.getInt());
            ddsHeader.setDwDepth(buffer.getInt());
            ddsHeader.setDwMipMapCount(buffer.getInt());
            buffer.position(buffer.position() + 44);

            /**
             * Pixel Format
             */
            DDSPixelFormat pfm = ddsHeader.getDwPixelFormat();
            pfm.setDwSize(buffer.getInt());
            if (pfm.getDwSize() != 32)
                throw new IOException("Pixel Format Size != 32");
            pfm.setDwFlags(buffer.getInt());
            System.out.println("PFM Size: " + pfm.getDwSize() + " Flags: " + pfm.getDwFlags());
            pfm.populateFlags();
            if (pfm.getFlags().contains(DDSPixelFormatFlags.DDPF_FOURCC)) {
                pfm.setDwFourCC(buffer.getInt());
                buffer.position(buffer.position() + 20);
                System.out.println("FCC: " + pfm.getDwFourCC());
                pfm.setDwFourCCString(StringUtil.intToString(pfm.getDwFourCC()));
                this.compressed = true;
                switch(pfm.getDwFourCC()) {
                    case PF_DXT1:
                        pfm.setDwRGBBitCount(4);
                        if (pfm.getFlags().contains(DDSPixelFormatFlags.DDPF_ALPHAPIXELS))
                            format = DDSFormat.DXT1;
                        else
                            format = DDSFormat.DXT1;
                        break;
                    case PF_DXT3:
                        pfm.setDwRGBBitCount(8);
                        format = DDSFormat.DXT3;
                        break;
                    case PF_DXT5:
                        pfm.setDwRGBBitCount(8);
                        format = DDSFormat.DXT5;
                        break;
                }
                int size = ((ddsHeader.getDwWidth() + 3) / 4) * ((ddsHeader.getDwHeight() + 3) / 4) * pfm.getDwRGBBitCount() * 2;
                if (ddsHeader.getFlags().contains(DDSHeaderFlags.DDSD_LINEARSIZE)) {
                    if (ddsHeader.getDwPitchOrLinearSize() == 0)
                        ddsHeader.setDwPitchOrLinearSize(size);
                    else if (ddsHeader.getDwPitchOrLinearSize() != size)
                        System.out.println("Unexpected Pitch Size: " + ddsHeader.getDwPitchOrLinearSize() + " Expected: " + size);
                }
            } else {
                System.out.println("Type: Uncompressed RGB");
                compressed = false;
                buffer.getInt();
                pfm.setDwRGBBitCount(buffer.getInt());
                pfm.setDwRBitMask(buffer.getInt());
                pfm.setDwGBitMask(buffer.getInt());
                pfm.setDwBBitMask(buffer.getInt());
                pfm.setDwABitMask(buffer.getInt());

                if (pfm.getFlags().contains(DDSPixelFormatFlags.DDPF_RGB)) {
                    if (pfm.getFlags().contains(DDSPixelFormatFlags.DDPF_ALPHAPIXELS))
                        format = DDSFormat.RGBA8;
                    else
                        format = DDSFormat.RGB8;
                } else if (pfm.getFlags().contains(DDSPixelFormatFlags.DDPF_LUMINANCE)) {
                    switch(pfm.getDwRGBBitCount()) {
                        case 4:  format = DDSFormat.LUMINANCE4; break;
                        case 8:  format = DDSFormat.LUMINANCE8; break;
                        case 12: format = DDSFormat.LUMINANCE12; break;
                        case 16: format = DDSFormat.LUMINANCE16; break;
                    }
                    grayscaleOrAlpha = true;
                } else if (pfm.getFlags().contains(DDSPixelFormatFlags.DDPF_ALPHA)) {
                    switch(pfm.getDwRGBBitCount()) {
                        case 4:  format = DDSFormat.ALPHA4; break;
                        case 8:  format = DDSFormat.ALPHA8; break;
                        case 12: format = DDSFormat.ALPHA12; break;
                        case 16: format = DDSFormat.ALPHA16; break;
                    }
                    grayscaleOrAlpha = true;
                }
                int size = (pfm.getDwRGBBitCount() / 8 * ddsHeader.getDwWidth());
                if (ddsHeader.getFlags().contains(DDSHeaderFlags.DDSD_LINEARSIZE)) {
                    if (ddsHeader.getDwPitchOrLinearSize() == 0)
                        ddsHeader.setDwPitchOrLinearSize(size);
                    else if (ddsHeader.getDwPitchOrLinearSize() != size)
                        System.out.println("Unexpected Pitch Size: " + ddsHeader.getDwPitchOrLinearSize() + " Expected: " + size);
                }
            }

            ddsHeader.setDwCaps(buffer.getInt());
            ddsHeader.setDwCaps2(buffer.getInt());
            buffer.position(buffer.position() + 12);

            if((ddsHeader.getDwCaps() & DDSCapsFlags.DDSCAPS_TEXTURE.getFlag()) != DDSCapsFlags.DDSCAPS_TEXTURE.getFlag())
                throw new IOException("Only textures supported.");

            int expectedMipmaps = 1 + (int) Math.ceil(Math.log(Math.max(ddsHeader.getDwHeight(), ddsHeader.getDwWidth())) / Math.log(2));
            if((ddsHeader.getDwCaps() & DDSCapsFlags.DDSCAPS_MIPMAP.getFlag()) != DDSCapsFlags.DDSCAPS_MIPMAP.getFlag()) {
                if(!ddsHeader.getFlags().contains(DDSHeaderFlags.DDSD_MIPMAPCOUNT))
                    ddsHeader.setDwMipMapCount(expectedMipmaps);
                else if(ddsHeader.getDwMipMapCount() != expectedMipmaps) {
                    System.out.println("Unpexpected MipMap Count: " + ddsHeader.getDwMipMapCount() + " Expected: " + expectedMipmaps);
                }
            } else {
                ddsHeader.setDwMipMapCount(1);
            }

            populateMipmapSizes();

            //TODO: parse the compressed or uncompressed pixel data.
            //max(1, ( (width + 3) / 4 ) ) x max(1, ( (height + 3) / 4 ) ) x 8(DXT1) or 16(DXT2-5)
            Decompressor decompressor = format == DDSFormat.DXT3 ? new DXT3Decompressor() : format == DDSFormat.DXT1 ? new DXT1Decompressor() :
                    format == DDSFormat.DXT5 ? new DXT5Decompressor() : null;
            int blockSize = format == DDSFormat.DXT1 ? 8 : 16;
            int pitch = Math.max(1, (ddsHeader.getDwWidth() + 3) / 4) * blockSize;
            String sFormat = format == DDSFormat.DXT1 ? "DXT1" : format == DDSFormat.DXT3 ? "DXT3" : format == DDSFormat.DXT5 ? "DXT5" : "Uknown";
            int expectedSize = Math.max(1, (ddsHeader.getDwWidth() + 3) / 4) * (Math.max(1, (ddsHeader.getDwHeight() + 3) / 4) * blockSize);
            System.out.println("Buffer: cap: " + buffer.capacity() + " pos: " + buffer.position() + " length: " + buffer.array().length + " Rem: " + (buffer.array().length - buffer.position()));
            System.out.println("Expected Size: " + expectedSize + " Format: " + sFormat + " Pitch: " + pitch);
            if(decompressor != null) {
                this.result = decompressor.decompress(buffer, ddsHeader.getDwWidth(), ddsHeader.getDwHeight());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Populate an array of mipmap sizes at each level.
     */
    private void populateMipmapSizes() {
        int width = ddsHeader.getDwWidth();
        int height = ddsHeader.getDwHeight();
        mipmapSizes = new int[ddsHeader.getDwMipMapCount()];
        for(int i = 0; i < ddsHeader.getDwMipMapCount(); i++) {
            int size;
            if(compressed)
                size = ((width + 3) / 4) * ((height + 3) / 4) * ddsHeader.getDwPixelFormat().getDwRGBBitCount() * 2;
            else
                size = width * height * ddsHeader.getDwPixelFormat().getDwRGBBitCount() / 8;

            mipmapSizes[i] = ((size + 3) / 4) * 4;
            width = Math.max(width / 2, 1);
            height = Math.max(height / 2, 1);
        }
    }

    /**
     * Retrieve the dds header for this file.
     * @return
     */
    public DDSHeader getHeader() {
        return ddsHeader;
    }

    public BufferedImage getResult() {
        return result;
    }
}
