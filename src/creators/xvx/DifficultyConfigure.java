package creators.xvx;

public class DifficultyConfigure {
    public float ʱ�䱶��(int ֵ){
        switch (ֵ){
            case 1 : return 2;
            case 2 : return 1.5f;
            case 3 : return 1;
            case 4 : return 0.8f;
            case 5 : return 2.5f;
        }
        return 1f;
    }

    public float ��λѪ������(int ֵ){
        switch (ֵ){
            case 1 : return 0.4f;
            case 2 : return 0.7f;
            case 3 : return 1;
            case 4 : return 1.8f;
            case 5 : return 3.3f;
        }
        return 1f;
    }

    public float ��λ��������(int ֵ){
        switch (ֵ){
            case 1 : return 0.5f;
            case 2 : return 0.75f;
            case 3 : return 1;
            case 4 : return 1.15f;
            case 5 : return 1.5f;
        }
        return 1f;
    }

    public float ��λ���ٱ���(int ֵ){
        switch (ֵ){
            case 1 : return 0.6f;
            case 2 : return 0.8f;
            case 3 : return 1;
            case 4 : return 1.5f;
            case 5 : return 2f;
        }
        return 1f;
    }

    public float ��λ�ƶ�����(int ֵ){
        switch (ֵ){
            case 1 : return 0.8f;
            case 2 : return 1f;
            case 3 : return 1f;
            case 4 : return 0.8f;
            case 5 : return 0.65f;
        }
        return 1f;
    }
}
