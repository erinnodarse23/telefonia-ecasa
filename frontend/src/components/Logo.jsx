import logoSrc from '../images/LogoECASA.SA.jpeg';

const containerSizes = { small: 46, medium: 62, large: 88 };
const imgSizes = { small: 38, medium: 52, large: 76 };

const Logo = ({ size = 'medium', className, onClick, collapsed }) => {
  const box = containerSizes[size] || containerSizes.medium;
  const px = imgSizes[size] || imgSizes.medium;

  return (
    <div
      className={className}
      onClick={(e) => {
        e.currentTarget.style.transform = 'scale(0.92)';
        setTimeout(() => { e.currentTarget.style.transform = 'scale(1)'; }, 150);
        onClick?.(e);
      }}
      style={{
        width: box,
        height: box,
        borderRadius: '50%',
        background: 'rgba(255,255,255,0.95)',
        boxShadow: collapsed
          ? '0 0 0 3px rgba(255,255,255,0.7), 0 0 0 6px rgba(255,255,255,0.15), 0 4px 14px rgba(0,0,0,0.2)'
          : '0 2px 8px rgba(0,0,0,0.15), 0 0 0 1px rgba(255,255,255,0.3)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: onClick ? 'pointer' : undefined,
        transition: 'transform 0.15s ease, box-shadow 0.35s ease',
        flexShrink: 0,
      }}
      title={collapsed ? 'Expandir menú' : 'Colapsar menú'}
    >
      <img
        src={logoSrc}
        alt="ECASA"
        style={{
          width: px,
          height: 'auto',
          objectFit: 'contain',
          display: 'block',
          borderRadius: 2,
        }}
      />
    </div>
  );
};

export default Logo;
