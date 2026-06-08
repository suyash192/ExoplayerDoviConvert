use dolby_vision::rpu::dovi_rpu::DoviRpu;
use jni::objects::{JByteArray, JClass};
use jni::sys::{jboolean, jbyteArray, jint, jlong, jstring};
use jni::JNIEnv;

use dolby_vision::rpu::rpu_data_nlq::DoviELType;

/// Bindings for dovi_parse_unspec62_nalu
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_libdovi_LibDovi_parseUnspec62Nalu(
    env: JNIEnv,
    _class: JClass,
    rpu_bytes: JByteArray,
) -> jlong {
    let rpu_data = match env.convert_byte_array(&rpu_bytes) {
        Ok(bytes) => bytes,
        Err(_) => return 0,
    };

    match DoviRpu::parse_unspec62_nalu(&rpu_data) {
        Ok(rpu) => {
            let boxed_rpu = Box::new(rpu);
            Box::into_raw(boxed_rpu) as jlong
        }
        Err(_) => 0,
    }
}

/// Bindings for dovi_convert_rpu_with_mode
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_libdovi_LibDovi_convertRpuWithMode(
    _env: JNIEnv,
    _class: JClass,
    rpu_ptr: jlong,
    mode: jint,
) -> jboolean {
    if rpu_ptr == 0 {
        return jni::sys::JNI_FALSE;
    }

    let rpu = unsafe { &mut *(rpu_ptr as *mut DoviRpu) };

    match rpu.convert_with_mode(mode as u8) {
        Ok(_) => jni::sys::JNI_TRUE,
        Err(_) => jni::sys::JNI_FALSE,
    }
}

/// Bindings for dovi_write_unspec62_nalu
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_libdovi_LibDovi_writeUnspec62Nalu<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    rpu_ptr: jlong,
) -> jbyteArray {
    if rpu_ptr == 0 {
        return std::ptr::null_mut();
    }

    let rpu = unsafe { &*(rpu_ptr as *mut DoviRpu) };

    match rpu.write_hevc_unspec62_nalu() {
        Ok(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(java_array) => java_array.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Bindings for dovi_rpu_free
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_libdovi_LibDovi_freeRpu(
    _env: JNIEnv,
    _class: JClass,
    rpu_ptr: jlong,
) {
    if rpu_ptr != 0 {
        unsafe {
            let _ = Box::from_raw(rpu_ptr as *mut DoviRpu);
        }
    }
}

/// Bindings to retrieve dovi_profile from the rpu
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_libdovi_LibDovi_getDoviProfile(
    _env: JNIEnv,
    _class: JClass,
    rpu_ptr: jlong,
) -> jint {
    if rpu_ptr == 0 {
        return -1;
    }

    let rpu = unsafe { &*(rpu_ptr as *mut DoviRpu) };

    rpu.dovi_profile as jint
}

/// Bindings to retrieve el_type from the rpu
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_libdovi_LibDovi_getElType<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    rpu_ptr: jlong,
) -> jstring {
    if rpu_ptr == 0 {
        return std::ptr::null_mut();
    }

    let rpu = unsafe { &*(rpu_ptr as *mut DoviRpu) };

    let el_type_str = match rpu.el_type {
        Some(DoviELType::MEL) => "MEL",
        Some(DoviELType::FEL) => "FEL",
        None => "NONE",
    };

    match env.new_string(el_type_str) {
        Ok(java_string) => java_string.into_raw(),
        Err(_) => {
            env.exception_clear().unwrap();
            std::ptr::null_mut()
        }
    }
}
