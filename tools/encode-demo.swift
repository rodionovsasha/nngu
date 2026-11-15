import AVFoundation
import CoreGraphics
import CoreVideo
import Foundation
import ImageIO
import UniformTypeIdentifiers

/// macOS encoder: only a video input is created; there is no microphone or audio track.
@main
struct DemoEncoder {
    static func main() async throws {
        let arguments = CommandLine.arguments
        guard arguments.count == 4 else {
            throw failure("Usage: encode-demo <frames-directory> <output.mp4> <poster.png>")
        }
        let directory = URL(fileURLWithPath: arguments[1], isDirectory: true)
        let output = URL(fileURLWithPath: arguments[2])
        let poster = URL(fileURLWithPath: arguments[3])
        let width = 1360, height = 880, fps: Int32 = 30, count = 900
        // Validate the entire sequence before creating an output file.
        for index in 0..<count {
            let file = directory.appendingPathComponent(String(format: "%04d.png", index))
            guard FileManager.default.fileExists(atPath: file.path) else {
                throw failure("Missing frame: \(file.path)")
            }
        }
        let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
        writer.shouldOptimizeForNetworkUse = true
        let input = AVAssetWriterInput(mediaType: .video, outputSettings: [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: width,
            AVVideoHeightKey: height,
            AVVideoCompressionPropertiesKey: [
                AVVideoAverageBitRateKey: 1_600_000,
                AVVideoExpectedSourceFrameRateKey: fps,
                AVVideoMaxKeyFrameIntervalKey: 60,
                AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel
            ]
        ])
        input.expectsMediaDataInRealTime = false
        let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input,
            sourcePixelBufferAttributes: [
                kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
                kCVPixelBufferWidthKey as String: width,
                kCVPixelBufferHeightKey as String: height,
                kCVPixelBufferCGImageCompatibilityKey as String: true,
                kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
            ])
        guard writer.canAdd(input) else { throw failure("H.264 encoder unavailable") }
        writer.add(input)
        guard writer.startWriting() else { throw writer.error ?? failure("Cannot start encoder") }
        writer.startSession(atSourceTime: .zero)
        guard let pool = adaptor.pixelBufferPool else { throw failure("Pixel buffer pool unavailable") }
        for index in 0..<count {
            while !input.isReadyForMoreMediaData {
                guard writer.status == .writing else { throw writer.error ?? failure("Encoder stopped") }
                try await Task.sleep(for: .milliseconds(2))
            }
            try autoreleasepool {
                let file = directory.appendingPathComponent(String(format: "%04d.png", index))
                guard let source = CGImageSourceCreateWithURL(file as CFURL, nil),
                      let image = CGImageSourceCreateImageAtIndex(source, 0, nil),
                      image.width == width, image.height == height else {
                    throw failure("Invalid frame dimensions: \(file.path)")
                }
                var buffer: CVPixelBuffer?
                guard CVPixelBufferPoolCreatePixelBuffer(nil, pool, &buffer) == kCVReturnSuccess,
                      let buffer else { throw failure("Cannot allocate pixel buffer") }
                CVPixelBufferLockBaseAddress(buffer, [])
                defer { CVPixelBufferUnlockBaseAddress(buffer, []) }
                let bitmapInfo = CGBitmapInfo.byteOrder32Little.rawValue | CGImageAlphaInfo.premultipliedFirst.rawValue
                guard let context = CGContext(data: CVPixelBufferGetBaseAddress(buffer), width: width,
                    height: height, bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
                    space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: bitmapInfo) else {
                    throw failure("Cannot create bitmap context")
                }
                context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
                guard adaptor.append(buffer, withPresentationTime: CMTime(value: Int64(index), timescale: fps)) else {
                    throw writer.error ?? failure("Cannot encode frame \(index)")
                }
            }
            if index % 150 == 0 { print("Encoded \(index)/\(count) frames") }
        }
        writer.endSession(atSourceTime: CMTime(value: Int64(count), timescale: fps))
        input.markAsFinished()
        await writer.finishWriting()
        guard writer.status == .completed else { throw writer.error ?? failure("Encoding failed") }

        let asset = AVURLAsset(url: output)
        let duration = try await asset.load(.duration).seconds
        let audio = try await asset.loadTracks(withMediaType: .audio)
        let video = try await asset.loadTracks(withMediaType: .video)
        guard abs(duration - 30) < 0.001, audio.isEmpty, video.count == 1 else {
            throw failure("Unexpected media tracks or duration: \(duration) s, \(audio.count) audio tracks")
        }
        let bytes = try output.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0
        guard bytes < 10_000_000 else { throw failure("Video exceeds the GitHub Free attachment limit: \(bytes) bytes") }

        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        let (image, _) = try await generator.image(at: CMTime(seconds: 2, preferredTimescale: fps))
        guard let destination = CGImageDestinationCreateWithURL(poster as CFURL, UTType.png.identifier as CFString, 1, nil) else {
            throw failure("Cannot create video poster")
        }
        CGImageDestinationAddImage(destination, image, nil)
        guard CGImageDestinationFinalize(destination) else { throw failure("Cannot save video poster") }
        print("Verified: \(duration) s, \(width)×\(height), \(fps) fps, H.264, 0 audio tracks, \(bytes) bytes")
        print(output.path)
    }

    private static func failure(_ message: String) -> NSError {
        NSError(domain: "QuantumWellDemo", code: 1, userInfo: [NSLocalizedDescriptionKey: message])
    }
}
