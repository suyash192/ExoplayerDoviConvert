use dolby_vision::rpu::dovi_rpu::DoviRpu;
use jni::objects::{JByteBuffer, JClass, JIntArray};
use jni::sys::{jboolean, jint, jintArray};
use jni::JNIEnv;

use dolby_vision::rpu::rpu_data_nlq::DoviELType;
use memchr::memmem;

/// Struct to hold complete information about a found NALU
#[derive(Debug, Clone, Copy)]
pub struct NaluInfo {
    pub start_code_idx: usize,
    pub payload_idx: usize,
    pub end_idx: usize,
    pub nalu_type: u8,
}

/// Helper function to find the next start code and payload start indices.
#[inline]
fn find_start_code(data: &[u8], offset: usize) -> Option<(usize, usize)> {
    if offset >= data.len() {
        return None;
    }

    let search_slice = &data[offset..];

    let pos = memmem::find(search_slice, b"\x00\x00\x01")?;

    let match_idx = offset + pos;

    // Check backwards for the optional 4th leading zero (0x00 00 00 01)
    let start_idx = if match_idx > 0 && data[match_idx - 1] == 0 {
        match_idx - 1
    } else {
        match_idx
    };

    Some((start_idx, match_idx + 3))
}

/// Scans the provided byte slice starting at `offset` to find the next complete NALU.
#[inline]
pub fn get_next_nalu(data: &[u8], offset: usize) -> Option<NaluInfo> {
    let (start_code_idx, payload_idx) = find_start_code(data, offset)?;

    let nalu_type = (data[payload_idx] & 0x7E) >> 1;

    let end_idx = match find_start_code(data, payload_idx) {
        Some((next_start_code_idx, _)) => next_start_code_idx,
        None => data.len(),
    };

    Some(NaluInfo {
        start_code_idx,
        payload_idx,
        end_idx,
        nalu_type,
    })
}

/// Checks if a given HEVC NALU payload contains HDR10+ metadata.
#[inline]
pub fn is_hdr10plus(nalu_data: &[u8]) -> bool {
    if nalu_data.len() < 10 {
        return false;
    }

    // The HDR10+ ITU-T T.35 Signature
    let hdr10plus_signature = [0xB5, 0x00, 0x3C, 0x00, 0x01, 0x04];

    // Skip the first 2 bytes (the header)
    nalu_data[2..]
        .windows(6)
        .any(|window| window == hdr10plus_signature)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_getRpuFrameInfo(
    env: JNIEnv,
    _class: JClass,
    frame_data: JByteBuffer,
    frame_size: jint,
) -> jintArray {
    let ptr = env
        .get_direct_buffer_address(&frame_data)
        .expect("Failed to get direct buffer address");

    let capacity = env
        .get_direct_buffer_capacity(&frame_data)
        .expect("Failed to get buffer capacity");

    let slice: &[u8] = unsafe { std::slice::from_raw_parts(ptr, capacity) };

    let valid_len = frame_size as usize;
    let mut offset = 0;

    let mut dovi_profile = 0;
    let mut el_type = 0;
    let mut has_hdr10plus = 0;

    while let Some(nalu) = get_next_nalu(&slice[..valid_len], offset) {
        if nalu.nalu_type == 62 {
            let payload = &slice[nalu.payload_idx..nalu.end_idx];

            let dovi_rpu = match DoviRpu::parse_unspec62_nalu(payload) {
                Ok(rpu) => rpu,
                Err(_) => continue,
            };

            dovi_profile = dovi_rpu.dovi_profile;

            el_type = match dovi_rpu.el_type {
                Some(DoviELType::FEL) => 2,
                Some(DoviELType::MEL) => 1,
                None => 0,
            };
        } else if nalu.nalu_type == 39 {
            let payload = &slice[nalu.payload_idx..nalu.end_idx];

            if is_hdr10plus(payload) {
                has_hdr10plus = 1;
            }
        }

        offset = nalu.end_idx;
    }

    let result_data: [i32; 3] = [dovi_profile as i32, el_type as i32, has_hdr10plus as i32];

    let array: JIntArray = env.new_int_array(3).expect("Failed to create int array");

    env.set_int_array_region(&array, 0, &result_data)
        .expect("Failed to set array region");

    array.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_processHevcFrame(
    env: JNIEnv,
    _class: JClass,
    frame_data: JByteBuffer,
    frame_size: jint,
    dovi_transform: jint,
    needs_hdr10plus_strip: jboolean,
) -> jint {
    let needs_dovi_conversion = dovi_transform == 1;
    let needs_dovi_strip = dovi_transform == 2;
    let needs_hdr10plus_strip_bool = needs_hdr10plus_strip != 0;

    let ptr = env
        .get_direct_buffer_address(&frame_data)
        .expect("Failed to get direct buffer address");

    let capacity = env
        .get_direct_buffer_capacity(&frame_data)
        .expect("Failed to get buffer capacity");

    let slice: &mut [u8] = unsafe { std::slice::from_raw_parts_mut(ptr, capacity) };

    let mut valid_len = frame_size as usize;
    let mut offset = 0;

    while let Some(nalu) = get_next_nalu(&slice[..valid_len], offset) {
        if nalu.nalu_type == 62 {
            if needs_dovi_conversion {
                let payload = &slice[nalu.payload_idx..nalu.end_idx];
                let new_payload = convert_to_p8(payload);

                let old_len = payload.len();
                let new_len = new_payload.len();

                if new_len > old_len {
                    let diff = new_len as isize - old_len as isize;
                    let dest_start = (nalu.end_idx as isize + diff) as usize;

                    slice.copy_within(nalu.end_idx..valid_len, dest_start);
                    valid_len = (valid_len as isize + diff) as usize;
                } else if new_len < old_len {
                    add_hevc_filler(slice, nalu.payload_idx + new_len, nalu.end_idx);
                }

                slice[nalu.payload_idx..nalu.payload_idx + new_len].copy_from_slice(&new_payload);

                offset = nalu.payload_idx + new_len;
            } else if needs_dovi_strip {
                add_hevc_filler(slice, nalu.payload_idx, nalu.end_idx);
                offset = nalu.end_idx;
            }
        } else if nalu.nalu_type == 39 && needs_hdr10plus_strip_bool {
            add_hevc_filler(slice, nalu.payload_idx, nalu.end_idx);
            offset = nalu.end_idx;
        } else {
            offset = nalu.end_idx;
        }
    }

    valid_len as jint
}

/// Fills the space from start till end with NALU 38 data
pub fn add_hevc_filler(buffer: &mut [u8], start: usize, end: usize) {
    let gap = end - start;

    if gap < 6 {
        // Annex B Zero-Padding for gaps too small for a NALU header (1 to 5 bytes)
        buffer[start..end].fill(0x00);
    } else {
        // Standard HEVC Filler Data (Type 38) for larger gaps
        buffer[start..start + 4].copy_from_slice(&[0x00, 0x00, 0x00, 0x01]);
        buffer[start + 4..start + 6].copy_from_slice(&[0x4C, 0x01]);

        if gap > 6 {
            buffer[start + 6..end].fill(0xFF);
        }
    }
}

#[inline]
fn convert_to_p8(payload: &[u8]) -> Vec<u8> {
    let mut dovi_rpu = match DoviRpu::parse_unspec62_nalu(payload) {
        Ok(rpu) => rpu,
        Err(_) => return payload.to_vec(),
    };

    if dovi_rpu.convert_with_mode(2).is_err() {
        return payload.to_vec();
    }

    match dovi_rpu.write_hevc_unspec62_nalu() {
        Ok(new_payload) => new_payload,
        Err(_) => payload.to_vec(),
    }
}
